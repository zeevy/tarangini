/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTest {
    @Test
    fun `minutes and seconds, and hours from one hour on`() {
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("3:05", formatDuration(185_999))
        assertEquals("59:59", formatDuration(3_599_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
    }

    @Test
    fun `a length the phone did not give is an empty text`() {
        assertEquals("", formatDuration(0))
        assertEquals("", formatDuration(-1))
    }
}
