/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import org.junit.Assert.assertEquals
import org.junit.Test

class CrossfadePlanTest {
    private val song = FadeSong(durationUs = 200_000_000, album = "Morning Ragas", isStream = false)
    private val other = FadeSong(durationUs = 180_000_000, album = "Evening Ragas", isStream = false)

    @Test
    fun `two songs of different albums fade for the set length`() {
        assertEquals(4_000_000, fadeUsFor(4_000, song, other, repeatOne = false, speed = 1f))
    }

    @Test
    fun `with no length set there is no fade`() {
        assertEquals(0, fadeUsFor(0, song, other, repeatOne = false, speed = 1f))
    }

    @Test
    fun `two songs of one album run into each other with no fade`() {
        assertEquals(0, fadeUsFor(4_000, song, song.copy(durationUs = 1_000_000_000), repeatOne = false, speed = 1f))
    }

    @Test
    fun `songs with no album name still fade`() {
        val loose = song.copy(album = null)

        assertEquals(4_000_000, fadeUsFor(4_000, loose, loose, repeatOne = false, speed = 1f))
    }

    @Test
    fun `there is no fade at the end of the queue, with repeat one, at another speed or for a station`() {
        assertEquals(0, fadeUsFor(4_000, song, null, repeatOne = false, speed = 1f))
        assertEquals(0, fadeUsFor(4_000, song, other, repeatOne = true, speed = 1f))
        assertEquals(0, fadeUsFor(4_000, song, other, repeatOne = false, speed = 1.25f))
        assertEquals(0, fadeUsFor(4_000, song, other.copy(isStream = true), repeatOne = false, speed = 1f))
    }

    @Test
    fun `a fade takes at most half of a short song and at most 12 seconds`() {
        val short = other.copy(durationUs = 3_000_000)

        assertEquals(1_500_000, fadeUsFor(4_000, song, short, repeatOne = false, speed = 1f))
        assertEquals(12_000_000, fadeUsFor(60_000, song, other, repeatOne = false, speed = 1f))
    }
}
