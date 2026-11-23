/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import com.siliconcircuits.tarangini.core.domain.audio.LevelReading
import com.siliconcircuits.tarangini.core.domain.audio.SILENCE_DB
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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LevelMeterTest {
    private val scope = TestScope()
    private val meter =
        LevelMeter(
            scope.backgroundScope,
            SoundVisualMeter(scope.backgroundScope, StandardTestDispatcher(scope.testScheduler)),
        )

    private fun processor(channels: Int, encoding: Int = C.ENCODING_PCM_16BIT) = meter.processor().apply {
        configure(AudioFormat(RATE, channels, encoding))
        flush(StreamMetadata.DEFAULT)
    }

    /** Collects the levels in the background and gives the readings seen so far. */
    private fun TestScope.watch(): List<LevelReading> {
        val seen = mutableListOf<LevelReading>()
        backgroundScope.launch { meter.levels().collect(seen::add) }
        runCurrent()
        return seen
    }

    @Test
    fun `a known tone gives its peak in dB on each side`() = scope.runTest {
        val seen = watch()
        // Left at half of full scale (-6.02 dB), right at a quarter (-12.04 dB).
        processor(2).queueInput(shorts(stereoTone(left = 0.5, right = 0.25)))
        advanceTimeBy(TICK_MS + 1)

        val reading = seen.last()
        assertEquals(-6.02f, reading.leftDb, 0.05f)
        assertEquals(-12.04f, reading.rightDb, 0.05f)
    }

    @Test
    fun `a mono float song has the same level on both sides`() = scope.runTest {
        val seen = watch()
        val samples = FloatArray(FRAMES) { (0.1 * sin(2 * PI * TONE_HZ * it / RATE)).toFloat() }
        val input = ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.nativeOrder())
        input.asFloatBuffer().put(samples)

        processor(1, C.ENCODING_PCM_FLOAT).queueInput(input)
        advanceTimeBy(TICK_MS + 1)

        assertEquals(-20f, seen.last().leftDb, 0.05f)
        assertEquals(seen.last().leftDb, seen.last().rightDb)
    }

    @Test
    fun `the samples come out the same as they went in`() = scope.runTest {
        watch()
        val samples = stereoTone(left = 0.9, right = 0.3)
        val processor = processor(2)

        processor.queueInput(shorts(samples))
        val output = processor.output.order(ByteOrder.nativeOrder()).asShortBuffer()

        assertArrayEquals(samples, ShortArray(output.remaining()).also(output::get))
    }

    @Test
    fun `with no sound the level falls 20 dB a second to silence`() = scope.runTest {
        val seen = watch()
        processor(2).queueInput(shorts(stereoTone(left = 1.0, right = 1.0)))
        advanceTimeBy(TICK_MS + 1)
        val loud = seen.last().leftDb

        advanceTimeBy(1_000)
        assertEquals(loud - 20f, seen.last().leftDb, 1f)

        advanceTimeBy(3_000)
        assertEquals(LevelReading.SILENCE, seen.last())
        assertEquals(SILENCE_DB, seen.last().rightDb)
    }

    @Test
    fun `the sound is measured only while the levels are watched`() = scope.runTest {
        assertFalse(meter.isWatched)
        // Sound that came while nobody watched is not shown later.
        processor(2).queueInput(shorts(stereoTone(left = 1.0, right = 1.0)))

        val seen = mutableListOf<LevelReading>()
        val job = backgroundScope.launch { meter.levels().collect(seen::add) }
        runCurrent()
        assertTrue(meter.isWatched)
        advanceTimeBy(TICK_MS + 1)
        assertEquals(listOf(LevelReading.SILENCE), seen)

        job.cancel()
        runCurrent()
        assertFalse(meter.isWatched)
    }

    @Test
    fun `two screens that watch at once see the same level`() = scope.runTest {
        val first = watch()
        val second = watch()
        processor(2).queueInput(shorts(stereoTone(left = 0.5, right = 0.5)))
        advanceTimeBy(TICK_MS + 1)

        assertEquals(-6.02f, first.last().leftDb, 0.05f)
        assertEquals(first.last(), second.last())
    }

    @Test
    fun `a sample format the meter cannot read passes it by`() {
        assertEquals(AudioFormat.NOT_SET, meter.processor().configure(AudioFormat(RATE, 2, C.ENCODING_PCM_24BIT)))
        assertEquals(AudioFormat.NOT_SET, meter.processor().configure(AudioFormat(RATE, 0, C.ENCODING_PCM_16BIT)))
    }

    /** Stereo 16 bit samples of a sine, with its own peak on each side. */
    private fun stereoTone(left: Double, right: Double): ShortArray = ShortArray(FRAMES * 2) { index ->
        val wave = sin(2 * PI * TONE_HZ * (index / 2) / RATE)
        (wave * (if (index % 2 == 0) left else right) * Short.MAX_VALUE).toInt().toShort()
    }

    private fun shorts(samples: ShortArray): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder()).also {
            it.asShortBuffer().put(samples)
        }

    private companion object {
        const val RATE = 44_100

        // A tone of 441 Hz puts whole waves into the buffer, so its top is in the samples.
        const val TONE_HZ = 441.0
        const val FRAMES = RATE / 10
        const val TICK_MS = 40L
    }
}
