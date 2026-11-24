/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Crossfade in front of the audio sink of the one player (ADR 0063). The renderer tells how much of
 * the song is left and when the next song starts; [FadeMixer] holds the end and mixes it in. The
 * sink then sees the next song start early and moves its clock on by itself.
 *
 * With [fadeUs] at 0, or sound that is not 16 bit, every buffer goes on untouched, so gapless
 * albums play byte for byte as without it. When the next song has another sample rate or channel
 * count, the held end plays as it is and the sink takes the new format after it.
 */
// A forwarding sink is API that may still change.
@OptIn(UnstableApi::class)
internal class CrossfadeSink(sink: AudioSink, private val fadeUs: () -> Long) : ForwardingAudioSink(sink) {
    private val mixer = FadeMixer()
    private var format: Format? = null
    private val waiting = WaitingSound()
    private val write: (ByteBuffer, Long) -> Boolean = { buffer, timeUs -> super.handleBuffer(buffer, timeUs, 1) }
    private var waitingConfig: AudioSink.AudioSinkConfig? = null

    // True while the sink holds part of a buffer that went past the mixer: the renderer must hand
    // the same buffer again, so it must go the same way.
    private var passedInPart = false

    /**
     * Called by the renderer on the playback thread for each piece of sound. [remainingUs] tells how
     * much of the song is still to come, or null when it is not known; it is asked only while a fade
     * is planned and nothing is held yet.
     */
    fun songEndsIn(remainingUs: () -> Long?) {
        val canHold = mixer.isPassing && !passedInPart && waitingConfig == null && format.canFade()
        val fade = if (canHold) fadeUs() else 0
        if (fade > 0) remainingUs()?.let { mixer.songEndsIn(it, fade) }
    }

    /**
     * Called by the renderer on the playback thread before the first sound of the next song. When the
     * fade was turned off after the end was held, as by repeat one or a removed song, the held end
     * plays as it is.
     */
    fun nextSongStarts() {
        if (fadeUs() <= 0 && mixer.hasTail) waiting.add(mixer.releaseTail()) else mixer.nextSongStarts()
    }

    override fun configure(config: AudioSink.AudioSinkConfig) {
        val next = config.format
        if (mixer.hasTail && !next.sameShapeAs(format)) {
            // The held end belongs to the old format: it goes out first, the new format after it.
            waiting.add(mixer.releaseTail())
            waitingConfig = config
        } else {
            super.configure(config)
        }
        format = next
        mixer.configure(next.sampleRate, next.channelCount)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        val ready = waiting.offer(write)
        if (ready) takeWaitingConfig()
        return when {
            !ready -> false

            passedInPart || mixer.isPassing || !format.isPcm16() -> {
                val done = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
                passedInPart = !done
                done
            }

            else -> {
                val samples = ShortArray(buffer.remaining() / Short.SIZE_BYTES)
                buffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer().get(samples)
                buffer.position(buffer.limit())
                waiting.add(mixer.take(samples, presentationTimeUs))
                waiting.offer(write)
                true
            }
        }
    }

    override fun playToEndOfStream() {
        // Nothing follows: a held end goes out as it is. The renderer calls this until the sink has ended.
        if (mixer.hasTail) waiting.add(mixer.releaseTail())
        if (waiting.offer(write)) {
            takeWaitingConfig()
            super.playToEndOfStream()
        }
    }

    override fun isEnded(): Boolean = waiting.isEmpty() && waitingConfig == null && super.isEnded()

    override fun hasPendingData(): Boolean = !waiting.isEmpty() || super.hasPendingData()

    override fun flush() {
        mixer.flush()
        waiting.clear()
        passedInPart = false
        takeWaitingConfig()
        super.flush()
    }

    override fun reset() {
        flush()
        super.reset()
    }

    /** Gives the sink the format of the next song once the held end of the old one has gone out. */
    private fun takeWaitingConfig() {
        val config = waitingConfig ?: return
        waitingConfig = null
        super.configure(config)
    }
}

/** Sound made by the crossfade that waits for room in the sink, in the order it plays. */
internal class WaitingSound {
    private val pending = ArrayDeque<Pair<ByteBuffer, Long>>()

    /** True when nothing waits. */
    fun isEmpty(): Boolean = pending.isEmpty()

    /** Adds [taken] after what already waits. */
    fun add(taken: FadeMixer.Taken) {
        if (taken.samples.isEmpty()) return
        val out = ByteBuffer.allocateDirect(taken.samples.size * Short.SIZE_BYTES).order(ByteOrder.nativeOrder())
        out.asShortBuffer().put(taken.samples)
        pending.addLast(out to taken.timeUs)
    }

    /** Hands on what waits with [write]. False while [write] has no room for all of it yet. */
    fun offer(write: (ByteBuffer, Long) -> Boolean): Boolean {
        while (pending.isNotEmpty()) {
            val (buffer, timeUs) = pending.first()
            if (buffer.hasRemaining() && !write(buffer, timeUs)) return false
            pending.removeFirst()
        }
        return true
    }

    /** Forgets what waits. */
    fun clear() = pending.clear()
}

@OptIn(UnstableApi::class)
private fun Format?.isPcm16(): Boolean = this != null && pcmEncoding == C.ENCODING_PCM_16BIT

// Mono or stereo up to 96 kHz, so the held end of a 12 second fade stays under 5 MB.
private fun Format?.canFade(): Boolean =
    this != null && isPcm16() && sampleRate in 1..MAX_FADE_RATE && channelCount in 1..2

private const val MAX_FADE_RATE = 96_000

private fun Format.sameShapeAs(other: Format?): Boolean = other != null &&
    sampleRate == other.sampleRate &&
    channelCount == other.channelCount &&
    pcmEncoding == other.pcmEncoding
