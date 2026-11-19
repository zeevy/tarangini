/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class TaranginiBuildTest {
    @Test
    fun `the app uses the base package`() {
        assertEquals("com.siliconcircuits.tarangini", TaranginiBuild.namespaceFor(":app"))
    }

    @Test
    fun `a module namespace follows its path`() {
        assertEquals(
            "com.siliconcircuits.tarangini.feature.library",
            TaranginiBuild.namespaceFor(":feature:library"),
        )
        assertEquals("com.siliconcircuits.tarangini.core.data", TaranginiBuild.namespaceFor(":core:data"))
    }

    @Test
    fun `only the app module itself maps to the base package`() {
        assertEquals("com.siliconcircuits.tarangini.feature.app", TaranginiBuild.namespaceFor(":feature:app"))
    }
}
