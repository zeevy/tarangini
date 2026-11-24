/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** One channel at 1,000 frames a second, so one sample is one millisecond. */
class FadeMixerTest {
    private val mixer = FadeMixer().apply { configure(sampleRate = 1_000, channels = 1) }

    @Test
    fun `with no fade planned every sample goes on untouched`() {
        val samples = ShortArray(10) { (it * 100).toShort() }

        mixer.songEndsIn(remainingUs = 5_000, fadeUs = 0)
        val taken = mixer.take(samples, timeUs = 7)

        assertTrue(mixer.isPassing)
        assertTrue(taken.samples === samples)
        assertEquals(7, taken.timeUs)
    }

    @Test
    fun `the end is held only once it is within the fade`() {
        mixer.songEndsIn(remainingUs = 9_000, fadeUs = 4_000)
        assertTrue(mixer.isPassing)

        mixer.songEndsIn(remainingUs = 4_000, fadeUs = 4_000)
        val taken = mixer.take(ShortArray(4) { 1_000 }, timeUs = 0)

        assertEquals(0, taken.samples.size)
        assertTrue(mixer.hasTail)
    }

    @Test
    fun `a song that runs longer than planned lets its oldest samples go on, in order`() {
        mixer.songEndsIn(remainingUs = 3_000, fadeUs = 3_000)

        mixer.take(shorts(1, 2), timeUs = 10_000)
        val taken = mixer.take(shorts(3, 4, 5), timeUs = 12_000)

        assertArrayEquals(shorts(1, 2), taken.samples)
        assertEquals(10_000, taken.timeUs)
        // The newest three stay held and come out in order when nothing follows.
        val rest = mixer.releaseTail()
        assertArrayEquals(shorts(3, 4, 5), rest.samples)
        assertEquals(12_000, rest.timeUs)
        assertTrue(mixer.isPassing)
    }

    @Test
    fun `the held end fades out on a cosine while the next song fades in on a sine`() {
        mixer.songEndsIn(remainingUs = 4_000, fadeUs = 4_000)
        mixer.take(ShortArray(4) { 10_000 }, timeUs = 0)

        mixer.nextSongStarts()
        val mixed = mixer.take(ShortArray(6) { 10_000 }, timeUs = 4_000).samples

        // At the start only the old song; in the middle both at 71 %, which adds up to more than one.
        assertEquals(10_000, mixed[0].toInt())
        assertEquals(14_142, mixed[2].toInt())
        // After the held end the new song plays as it is.
        assertEquals(10_000, mixed[4].toInt())
        assertEquals(10_000, mixed[5].toInt())
        assertTrue(mixer.isPassing)
    }

    @Test
    fun `a mix that would go over full scale is held at the top`() {
        mixer.songEndsIn(remainingUs = 4_000, fadeUs = 4_000)
        mixer.take(ShortArray(4) { Short.MAX_VALUE }, timeUs = 0)
        mixer.nextSongStarts()

        val mixed = mixer.take(ShortArray(4) { Short.MAX_VALUE }, timeUs = 4_000).samples

        assertEquals(Short.MAX_VALUE, mixed[2])
    }

    @Test
    fun `a mix runs on over several buffers of the next song`() {
        mixer.songEndsIn(remainingUs = 4_000, fadeUs = 4_000)
        mixer.take(ShortArray(4) { 0 }, timeUs = 0)
        mixer.nextSongStarts()

        val first = mixer.take(ShortArray(2) { 10_000 }, timeUs = 4_000).samples
        val second = mixer.take(ShortArray(4) { 10_000 }, timeUs = 6_000).samples

        assertEquals(0, first[0].toInt())
        assertEquals(7_071, second[0].toInt())
        assertEquals(10_000, second[3].toInt())
    }

    @Test
    fun `when the next song stops in the middle of the fade the rest of the old end is still given`() {
        mixer.songEndsIn(remainingUs = 4_000, fadeUs = 4_000)
        mixer.take(shorts(1, 2, 3, 4), timeUs = 0)
        mixer.nextSongStarts()
        mixer.take(shorts(0), timeUs = 4_000)

        val rest = mixer.releaseTail()

        assertArrayEquals(shorts(2, 3, 4), rest.samples)
        assertEquals(1_000, rest.timeUs)
    }

    @Test
    fun `the ring keeps the order over many buffers`() {
        mixer.songEndsIn(remainingUs = 3_000, fadeUs = 3_000)
        val gone = (1..10).flatMap { mixer.take(shorts(it), timeUs = it * 1_000L).samples.toList() }

        assertEquals((1..7).map(Int::toShort), gone)
        assertArrayEquals(shorts(8, 9, 10), mixer.releaseTail().samples)
    }

    @Test
    fun `a seek forgets the held end`() {
        mixer.songEndsIn(remainingUs = 4_000, fadeUs = 4_000)
        mixer.take(ShortArray(4) { 5 }, timeUs = 0)

        mixer.flush()
        mixer.nextSongStarts()
        val samples = ShortArray(3) { 9 }

        assertTrue(mixer.take(samples, timeUs = 0).samples === samples)
    }

    @Test
    fun `two channels are held and mixed frame by frame`() {
        val stereo = FadeMixer().apply { configure(sampleRate = 1_000, channels = 2) }
        stereo.songEndsIn(remainingUs = 2_000, fadeUs = 2_000)
        stereo.take(shorts(10_000, -10_000, 10_000, -10_000), timeUs = 0)
        stereo.nextSongStarts()

        val mixed = stereo.take(shorts(0, 0, 0, 0), timeUs = 2_000).samples

        // Both samples of a frame get the same share: the first frame is all old song.
        assertArrayEquals(shorts(10_000, -10_000, 7_071, -7_071), mixed)
    }

    private fun shorts(vararg values: Int) = ShortArray(values.size) { values[it].toShort() }
}
