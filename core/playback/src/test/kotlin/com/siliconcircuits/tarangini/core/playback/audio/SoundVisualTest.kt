/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoundVisualTest {
    private val scope = TestScope()
    private val visual = SoundVisualMeter(scope.backgroundScope, StandardTestDispatcher(scope.testScheduler))
    private val meter = LevelMeter(scope.backgroundScope, visual)

    private fun tone(hz: Double, size: Int = SampleWindow.SIZE, level: Double = 1.0) =
        FloatArray(size) { (level * sin(2 * PI * hz * it / RATE)).toFloat() }

    @Test
    fun `a low tone lights a low band and a high tone a high band`() {
        val low = spectrumOf(tone(100.0), RATE)
        val high = spectrumOf(tone(8_000.0), RATE)

        assertTrue(low.indexOfMax() < VisualFrame.BAND_COUNT / 4)
        assertTrue(high.indexOfMax() > VisualFrame.BAND_COUNT * 3 / 4)
        assertEquals(VisualFrame.BAND_COUNT, low.size)
    }

    @Test
    fun `a full scale tone reaches the top, a tone 30 dB lower is near the middle and silence is zero`() {
        assertEquals(1f, spectrumOf(tone(1_000.0), RATE).max(), 0.1f)
        assertEquals(0.5f, spectrumOf(tone(1_000.0, level = 0.0316), RATE).max(), 0.1f)
        assertEquals(0f, spectrumOf(FloatArray(SampleWindow.SIZE), RATE).max())
    }

    @Test
    fun `the transform finds a single frequency in its own bin`() {
        val size = 64
        val real = FloatArray(size) { sin(2 * PI * 4 * it / size).toFloat() }
        val imaginary = FloatArray(size)

        fft(real, imaginary)

        val power = List(size / 2) { real[it] * real[it] + imaginary[it] * imaginary[it] }
        assertEquals(4, power.indexOfMax())
    }

    @Test
    fun `the wave has its points from the newest sound and stays within one`() {
        val wave = waveOf(FloatArray(SampleWindow.SIZE) { 2f })

        assertEquals(VisualFrame.WAVE_POINTS, wave.size)
        assertTrue(wave.all { it == 1f })
    }

    @Test
    fun `samples go to the visual only while it is watched, and the bars fall when the sound stops`() = scope.runTest {
        val processor = meter.processor().apply {
            configure(AudioFormat(RATE, 2, C.ENCODING_PCM_16BIT))
            flush(StreamMetadata.DEFAULT)
        }
        processor.queueInput(stereo(tone(1_000.0)))
        assertFalse(visual.isWatched)

        val seen = mutableListOf<VisualFrame>()
        backgroundScope.launch { visual.frames().collect(seen::add) }
        runCurrent()
        assertEquals(VisualFrame.SILENT, seen.single())

        processor.queueInput(stereo(tone(1_000.0)))
        advanceTimeBy(TICK_MS + 1)
        assertTrue(seen.last().bands.max() > 0.8f)
        assertTrue(seen.last().wave.any { it != 0f })

        visual.window.clear()
        advanceTimeBy(1_000)
        assertEquals(0f, seen.last().bands.max())
    }

    private fun List<Float>.indexOfMax(): Int = indices.maxBy { this[it] }

    private fun stereo(mono: FloatArray): ByteBuffer =
        ByteBuffer.allocateDirect(mono.size * 4).order(ByteOrder.nativeOrder()).also { buffer ->
            val shorts = buffer.asShortBuffer()
            mono.forEach { sample ->
                val value = (sample * Short.MAX_VALUE).toInt().toShort()
                shorts.put(value)
                shorts.put(value)
            }
        }

    private companion object {
        const val RATE = 44_100
        const val TICK_MS = 40L
    }
}
