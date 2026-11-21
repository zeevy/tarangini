/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun `the version code grows with the version name`() {
        assertEquals(1_000, TaranginiBuild.versionCodeOf("0.1.0"))
        assertEquals(1_004_002, TaranginiBuild.versionCodeOf("1.4.2"))
        assertEquals(1_999_999, TaranginiBuild.versionCodeOf("1.999.999"))
        assertTrue(TaranginiBuild.versionCodeOf("1.0.0") > TaranginiBuild.versionCodeOf("0.999.999"))
        assertEquals(2_000_999_999, TaranginiBuild.versionCodeOf("2000.999.999"))
    }

    @Test
    fun `a version name of another form fails the build`() {
        val refused = listOf(
            "1.0", "1.0.0.0", "1.0.x", "1.0.0-beta", "1.1000.0", "-1.0.0", "",
            "+1.0.0", "01.0.0", " 1.0.0", "1.-0.0", "2001.0.0",
        )
        refused.forEach { name ->
            assertThrows(name, IllegalArgumentException::class.java) { TaranginiBuild.versionCodeOf(name) }
        }
    }
}
