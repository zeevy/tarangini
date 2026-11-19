/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumBoundaryTest {
    @Test
    fun `an import of the premium package is found with its line number`() {
        val source = """
            package com.siliconcircuits.tarangini.feature.player

            import com.siliconcircuits.tarangini.premium.cast.CastButton
        """.trimIndent()

        assertEquals(listOf(3), PremiumBoundary.violations(source))
    }

    @Test
    fun `a fully qualified use without an import is found`() {
        val source = "val gate = com.siliconcircuits.tarangini.premium.RealPremiumGate()"

        assertEquals(listOf(1), PremiumBoundary.violations(source))
    }

    @Test
    fun `the premium interfaces of the domain are not the premium package`() {
        val source = "import com.siliconcircuits.tarangini.core.domain.premium.PremiumGate"

        assertEquals(emptyList<Int>(), PremiumBoundary.violations(source))
    }

    @Test
    fun `a package that only starts with the same letters is allowed`() {
        val source = "import com.siliconcircuits.tarangini.premiumhooks.Something"

        assertEquals(emptyList<Int>(), PremiumBoundary.violations(source))
    }

    @Test
    fun `clean code has no violation`() {
        val source = "package com.siliconcircuits.tarangini\n\nclass MainActivity"

        assertEquals(emptyList<Int>(), PremiumBoundary.violations(source))
    }
}
