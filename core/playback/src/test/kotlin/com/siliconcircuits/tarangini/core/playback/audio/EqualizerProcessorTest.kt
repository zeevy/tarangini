/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.StreamMetadata
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.audio.FIVE_BANDS_HZ
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerProcessorTest {
    private val processor = EqualizerProcessor().apply {
        configure(AudioFormat(RATE, 2, C.ENCODING_PCM_16BIT))
        flush(StreamMetadata.DEFAULT)
    }

    private fun bands(vararg gains: Float, bass: Float = 0f) = EqualizerTarget(FIVE_BANDS_HZ, gains.toList(), bass)

    @Test
    fun `flat gives back every sample as it was`() {
        val input = tone(440.0, 0.5, seconds = 0.1)

        assertArrayEquals(input, run(input))
    }

    @Test
    fun `one band lifts its own tone against the others`() {
        processor.setTarget(bands(0f, 0f, 6f, 0f, 0f))

        val lifted = level(run(tone(910.0, 0.25, seconds = 0.5)))
        processor.flush(StreamMetadata.DEFAULT)
        val alone = level(run(tone(60.0, 0.25, seconds = 0.5)))

        // The whole sound is turned down by the lift, so the band tone stays where it was and the rest drops.
        assertEquals(6.0, lifted - alone, 0.5)
    }

    @Test
    fun `the full boost of every band and of the bass does not clip a song at full level`() {
        processor.setTarget(bands(12f, 12f, 12f, 12f, 12f, bass = 12f))

        for (hz in listOf(40.0, 60.0, 230.0, 910.0, 3_600.0, 14_000.0)) {
            processor.flush(StreamMetadata.DEFAULT)
            val out = run(tone(hz, 0.9, seconds = 0.3))
            val peak = out.drop(out.size / 2).maxOf { abs(it.toInt()) }
            assertTrue("$hz Hz reached $peak", peak < Short.MAX_VALUE * 0.95)
        }
    }

    @Test
    fun `a change of a band while a tone plays makes no click`() {
        val input = tone(1_000.0, 0.5, seconds = 1.0)
        // Halfway, on the start of a stereo frame.
        val half = input.size / 4 * 2
        val first = run(input.copyOfRange(0, half))
        processor.setTarget(bands(0f, 0f, 0f, 12f, 0f))
        val second = run(input.copyOfRange(half, input.size))
        val out = first + second

        // A click is one step between two samples that is larger than the tone itself makes.
        val largestToneStep = 0.5 * Short.MAX_VALUE * 2 * PI * 1_000.0 / RATE
        val around = (half - 2 * FRAMES_PER_MS * 2)..(half + 2 * FRAMES_PER_MS * 2 * 30)
        val steps = around.filter { it % 2 == 0 && it >= 2 }.map { abs(out[it] - out[it - 2]) }
        assertTrue("largest step ${steps.max()}", steps.max() < largestToneStep * 1.2)
    }

    @Test
    fun `the number of samples never changes, so gapless playback stays as it is`() {
        processor.setTarget(
            targetOf(EqualizerSettings(isOn = true, preset = EqualizerPreset.CARNATIC, bassBoost = 50), FIVE_BANDS_HZ),
        )
        val input = tone(440.0, 0.5, seconds = 0.2)

        var frames = 0
        input.toList().chunked(1_000).forEach { part -> frames += run(part.toShortArray()).size }

        assertEquals(input.size, frames)
    }

    @Test
    fun `the setting stays after the player stops and starts again`() {
        processor.setTarget(bands(0f, 0f, 6f, 0f, 0f))
        val before = run(tone(910.0, 0.25, seconds = 0.2))

        processor.reset()
        processor.configure(AudioFormat(RATE, 2, C.ENCODING_PCM_16BIT))
        processor.flush(StreamMetadata.DEFAULT)
        val after = run(tone(910.0, 0.25, seconds = 0.2))

        assertArrayEquals(before, after)
    }

    @Test
    fun `a format other than 16 bit or float plays with no equalizer, not as an error`() {
        val other = EqualizerProcessor()

        assertEquals(AudioFormat.NOT_SET, other.configure(AudioFormat(RATE, 2, C.ENCODING_PCM_24BIT)))
        assertEquals(false, other.isActive)
    }

    @Test
    fun `a float sample that is no number does not silence the rest`() {
        val floats = EqualizerProcessor().apply {
            configure(AudioFormat(RATE, 1, C.ENCODING_PCM_FLOAT))
            flush(StreamMetadata.DEFAULT)
            setTarget(bands(0f, 0f, 6f, 0f, 0f))
        }
        val samples = FloatArray(1_000) { if (it == 10) Float.NaN else 0.25f }
        val input = ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.nativeOrder())
        input.asFloatBuffer().put(samples)

        floats.queueInput(input)
        val out = floats.output.asFloatBuffer()

        assertTrue((0 until out.remaining()).all { out.get(it).isFinite() })
    }

    @Test
    fun `a slider that moves fast makes no jump`() {
        val input = tone(1_000.0, 0.5, seconds = 0.5)
        val out = mutableListOf<Short>()
        // A new target every 5 ms, faster than the fade of 20 ms.
        input.toList().chunked(FRAMES_PER_MS * 2 * 5).forEachIndexed { index, part ->
            processor.setTarget(bands(0f, 0f, 0f, if (index % 2 == 0) 12f else -12f, 0f))
            out += run(part.toShortArray()).toList()
        }

        val largestToneStep = 0.5 * Short.MAX_VALUE * 2 * PI * 1_000.0 / RATE
        val steps = (2 until out.size step 2).map { abs(out[it] - out[it - 2]) }
        assertTrue("largest step ${steps.max()}", steps.max() < largestToneStep * 1.2)
    }

    @Test
    fun `the equalizer off is flat`() {
        assertEquals(
            EqualizerTarget.FLAT,
            targetOf(EqualizerSettings(isOn = false, preset = EqualizerPreset.BASS), FIVE_BANDS_HZ),
        )
    }

    @Test
    fun `a minute of stereo sound takes a small part of a minute`() {
        processor.setTarget(bands(3f, -2f, 4f, 2f, 5f, bass = 6f))
        val second = tone(440.0, 0.5, seconds = 1.0)
        run(second)

        val start = System.nanoTime()
        repeat(SECONDS_MEASURED) { run(second) }
        val millis = (System.nanoTime() - start) / 1_000_000

        // On a laptop this is about 1 % of the time of the sound; a phone is a few times slower.
        assertTrue("$millis ms for $SECONDS_MEASURED s of sound", millis < SECONDS_MEASURED * 1_000 / 10)
    }

    /** Stereo 16 bit samples of a sine at [hz], both channels the same. */
    private fun tone(hz: Double, level: Double, seconds: Double): ShortArray {
        val frames = (RATE * seconds).toInt()
        return ShortArray(frames * 2) { index ->
            (sin(2 * PI * hz * (index / 2) / RATE) * level * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun run(samples: ShortArray): ShortArray {
        val input = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.nativeOrder())
        input.asShortBuffer().put(samples)
        processor.queueInput(input)
        val output = processor.output.asShortBuffer()
        return ShortArray(output.remaining()).also(output::get)
    }

    /** The level of the second half in dB, after the filters settled. */
    private fun level(samples: ShortArray): Double {
        val half = samples.drop(samples.size / 2)
        val power = half.sumOf { it.toDouble() * it } / half.size
        return 10 * log10(power)
    }

    private companion object {
        const val RATE = 44_100
        const val FRAMES_PER_MS = RATE / 1_000
        const val SECONDS_MEASURED = 60
    }
}
