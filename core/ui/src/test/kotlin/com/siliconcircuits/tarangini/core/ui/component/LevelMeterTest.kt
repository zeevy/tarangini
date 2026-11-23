/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelMeterTest {
    @Test
    fun `silence lights nothing, full scale lights every segment, and the steps are even in dB`() {
        assertEquals(0, litSegments(-60f))
        assertEquals(0, litSegments(-90f))
        assertEquals(SEGMENTS, litSegments(0f))
        assertEquals(SEGMENTS, litSegments(3f))
        assertEquals(SEGMENTS / 2, litSegments(-30f))
        // -6 dB, a peak at half of full scale, lights 14 of 16 segments: the first orange one.
        assertEquals(14, litSegments(-6f))
    }
}
