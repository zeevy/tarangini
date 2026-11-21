/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.tarangini.core.domain.audio.ReplayGain
import java.nio.ByteBuffer

/**
 * The equalizer and the bass boost, as an audio processor of the player. It sounds the same on
 * every phone, because it does not use the effects of the phone.
 *
 * A new target is taken at the next buffer. The old and the new filters then both run for 20 ms
 * and the sound fades from one to the other, so a change has no click. The number of samples
 * never changes, so gapless playback stays as it is.
 */
// A processor of the audio sink is API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class EqualizerProcessor : BaseAudioProcessor() {
    @Volatile
    private var wanted = EqualizerTargets()

    @Volatile
    private var song = PlayingSong()
    private var seen: Pair<EqualizerTargets, PlayingSong>? = null
    private var applied: EqualizerTarget? = null
    private var current: FilterChain? = null
    private var fading: FilterChain? = null
    private var fadeFrames = 1
    private var fadeLeft = 0

    /** Asks for [target] for every song. Safe from any thread; it is heard from the next buffer. */
    fun setTarget(target: EqualizerTarget) = setTargets(EqualizerTargets(target))

    /** Asks for [targets]. Safe from any thread; it is heard from the next buffer. */
    fun setTargets(targets: EqualizerTargets) {
        wanted = targets
    }

    /**
     * The first sound of a song of [genreKeys] with the tags [gain] comes next. Called on the
     * playback thread between the last buffer of one song and the first of the next, and again
     * when the tags of the song that plays are read only after its start.
     */
    fun songStarts(genreKeys: List<String>, gain: ReplayGain? = null) {
        song = PlayingSong(genreKeys, gain)
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // The sink hands 16 bit or float samples to the processors. Any other format plays as it is,
        // with no equalizer, and not as an error.
        val isKnown =
            inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        return if (isKnown) inputAudioFormat else AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val chain = follow()
        val format = inputAudioFormat
        val output = replaceOutputBuffer(inputBuffer.remaining())
        if (chain.isIdentity && fading == null) {
            output.put(inputBuffer)
        } else {
            filter(inputBuffer, output, chain, format)
        }
        output.flip()
    }

    private fun filter(input: ByteBuffer, output: ByteBuffer, chain: FilterChain, format: AudioFormat) {
        val channels = format.channelCount
        val isFloat = format.encoding == C.ENCODING_PCM_FLOAT
        val frameBytes = format.bytesPerFrame
        while (input.remaining() >= frameBytes) {
            val old = fading
            val share = if (old == null) 1.0 else 1.0 - fadeLeft.toDouble() / fadeFrames
            for (channel in 0 until channels) {
                val read = if (isFloat) input.getFloat().toDouble() else input.getShort() / SHORT_SCALE
                // A sample that is no number would stay in the state of the filters and silence the rest.
                val sample = if (read.isFinite()) read else 0.0
                var value = chain.process(channel, sample)
                if (old != null) value = value * share + old.process(channel, sample) * (1.0 - share)
                if (isFloat) {
                    output.putFloat(value.coerceIn(-1.0, 1.0).toFloat())
                } else {
                    output.putShort(
                        (value * SHORT_SCALE).coerceIn(
                            Short.MIN_VALUE.toDouble(),
                            Short.MAX_VALUE.toDouble(),
                        ).toInt().toShort(),
                    )
                }
            }
            if (old != null && --fadeLeft <= 0) fading = null
        }
        // A part of a frame cannot be played; it is dropped as the sink would drop it.
        input.position(input.limit())
    }

    /**
     * The chain of the wanted target. A new target starts the fade from the chain before it. While
     * a fade runs the newest target waits for its end, so a slider that moves fast makes no jump.
     */
    private fun follow(): FilterChain {
        val now = current
        val asked = wanted to song
        return when {
            now != null && (fading != null || asked == seen) -> now

            else -> {
                seen = asked
                val target = asked.first.forSong(asked.second.genreKeys, asked.second.gain)
                if (now != null && target == applied) now else chainFor(target, now)
            }
        }
    }

    /** A new chain for [target]; with a chain [now] that plays, the fade from it starts. */
    private fun chainFor(target: EqualizerTarget, now: FilterChain?): FilterChain {
        val format = inputAudioFormat
        val next = FilterChain.of(target, format.sampleRate, format.channelCount)
        if (now != null) {
            next.takeStateFrom(now)
            fading = now
            fadeFrames = maxOf(1, format.sampleRate / FADES_PER_SECOND)
            fadeLeft = fadeFrames
        }
        current = next
        applied = target
        return next
    }

    override fun onFlush(streamMetadata: StreamMetadata) {
        // After a seek or a new format the old state means nothing; the next buffer builds a new chain.
        current = null
        applied = null
        fading = null
        seen = null
    }

    override fun onReset() {
        // The wanted target stays: the setting changes only when the person changes it, not when the
        // player stops.
        onFlush(StreamMetadata.DEFAULT)
    }

    private companion object {
        /** A fade of 20 ms. */
        const val FADES_PER_SECOND = 50

        /** 16 bit samples to -1 to 1 and back. */
        const val SHORT_SCALE = 32_768.0
    }
}

/** The song whose sound goes through the processor: its genres and its ReplayGain tags. */
private data class PlayingSong(val genreKeys: List<String> = emptyList(), val gain: ReplayGain? = null)
