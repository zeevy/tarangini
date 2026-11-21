/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiquadTest {
    @Test
    fun `a bell has the numbers of the cookbook`() {
        // 1 kHz, Q 1, +6 dB at 48 kHz, worked out by hand from the formulas of the cookbook.
        val bell = Biquad.peaking(48_000, 1_000.0, 1.0, 6.0)

        assertBiquad(Biquad(1.043953087, -1.895320724, 0.867722285, -1.895320724, 0.911675372), bell)
    }

    @Test
    fun `a low shelf has the numbers of the cookbook`() {
        // 120 Hz, slope 1, +12 dB at 48 kHz.
        val shelf = Biquad.lowShelf(48_000, 120.0, 12.0)

        assertBiquad(Biquad(1.007856353, -1.98409067, 0.976722793, -1.984273558, 0.984396257), shelf)
    }

    @Test
    fun `a bell lifts its own tone by its gain and leaves the far tones alone`() {
        val bell = Biquad.peaking(44_100, 910.0, 0.9, 6.0)

        assertEquals(6.0, bell.gainDbAt(910.0, 44_100), 0.01)
        assertEquals(0.0, bell.gainDbAt(20.0, 44_100), 0.05)
        assertEquals(0.0, bell.gainDbAt(20_000.0, 44_100), 0.05)
    }

    @Test
    fun `a low shelf lifts the bass and leaves the treble alone`() {
        val shelf = Biquad.lowShelf(44_100, 100.0, 12.0)

        assertEquals(12.0, shelf.gainDbAt(10.0, 44_100), 0.1)
        assertEquals(0.0, shelf.gainDbAt(10_000.0, 44_100), 0.05)
    }

    @Test
    fun `no gain is no filter`() {
        assertTrue(Biquad.peaking(44_100, 910.0, 0.9, 0.0).isIdentity)
        assertTrue(Biquad.lowShelf(44_100, 100.0, 0.0).isIdentity)
    }

    private fun assertBiquad(expected: Biquad, actual: Biquad) {
        assertEquals(expected.b0, actual.b0, 1e-8)
        assertEquals(expected.b1, actual.b1, 1e-8)
        assertEquals(expected.b2, actual.b2, 1e-8)
        assertEquals(expected.a1, actual.a1, 1e-8)
        assertEquals(expected.a2, actual.a2, 1e-8)
    }
}
