/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Measures the sound on its way to the speaker for [meter], and hands every sample on as it came.
 * It stands after the equalizer and volume levelling, and before the volume and the speed change of
 * the player, so a fade of the sleep timer does not show on the meter. While nobody watches the meter
 * it only copies the samples.
 */
// A processor of the audio sink is API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class LevelMeterProcessor(private val meter: LevelMeter) : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // The sink hands 16 bit or float samples to the processors. Any other format plays as it is,
        // with no meter, and not as an error.
        val isKnown = inputAudioFormat.channelCount > 0 &&
            (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT)
        return if (isKnown) inputAudioFormat else AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        if (meter.isWatched) measure(inputBuffer)
        val output = replaceOutputBuffer(inputBuffer.remaining())
        output.put(inputBuffer)
        output.flip()
    }

    // Reads a copy of the buffer, so the samples handed on are not touched.
    private fun measure(input: ByteBuffer) {
        val format = inputAudioFormat
        val samples = input.duplicate().order(input.order())
        val isFloat = format.encoding == C.ENCODING_PCM_FLOAT
        val channels = format.channelCount
        var left = 0f
        var right = 0f
        while (samples.remaining() >= format.bytesPerFrame) {
            for (channel in 0 until channels) {
                val read = if (isFloat) samples.getFloat() else samples.getShort() / SHORT_SCALE
                val size = if (read.isFinite()) abs(read) else 0f
                when (channel) {
                    0 -> left = maxOf(left, size)
                    1 -> right = maxOf(right, size)
                }
            }
        }
        // A mono song is heard on both sides. More than two channels show the front left and right.
        meter.report(left, if (channels == 1) left else right)
    }

    private companion object {
        const val SHORT_SCALE = 32_768f
    }
}
