/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellLayoutTest {
    @Test
    fun `a phone held upright gets the bottom bar`() {
        assertEquals(ShellLayout.COMPACT, ShellLayout.of(320.dp, 640.dp))
        assertEquals(ShellLayout.COMPACT, ShellLayout.of(599.dp, 900.dp))
    }

    @Test
    fun `from 600 dp the tabs move to the rail`() {
        assertEquals(ShellLayout.MEDIUM, ShellLayout.of(600.dp, 800.dp))
        assertEquals(ShellLayout.MEDIUM, ShellLayout.of(839.dp, 800.dp))
    }

    @Test
    fun `from 840 dp the now playing pane is added`() {
        assertEquals(ShellLayout.EXPANDED, ShellLayout.of(840.dp, 480.dp))
        assertEquals(ShellLayout.EXPANDED, ShellLayout.of(1_280.dp, 800.dp))
    }

    @Test
    fun `a large phone on its side is too low for the pane and gets the rail`() {
        assertEquals(ShellLayout.MEDIUM, ShellLayout.of(914.dp, 411.dp))
        assertEquals(ShellLayout.MEDIUM, ShellLayout.of(1_280.dp, 479.dp))
    }
}
