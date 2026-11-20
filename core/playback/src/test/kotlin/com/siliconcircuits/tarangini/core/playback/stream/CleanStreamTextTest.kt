/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanStreamTextTest {
    @Test
    fun `a normal title stays as it is, in any script`() {
        assertEquals("Ravi Shankar - Raga Bhairavi", cleanStreamText("Ravi Shankar - Raga Bhairavi"))
        assertEquals("ఘంటసాల - శివశంకరి", cleanStreamText("ఘంటసాల - శివశంకరి"))
    }

    @Test
    fun `white space is trimmed and made one space, and control characters go`() {
        assertEquals("One Two Three", cleanStreamText("  One\t\tTwo\r\n Three\u0000\u0007 "))
    }

    @Test
    fun `characters that turn the text around go, the joiners of Indian scripts stay`() {
        assertEquals("Song gnoS", cleanStreamText("Song \u202EgnoS\u202C\u200F"))
        assertEquals("క్\u200Cష", cleanStreamText("క్\u200Cష"))
    }

    @Test
    fun `a no-break space and a line separator count as white space`() {
        assertEquals("One Two Three", cleanStreamText("One\u00A0Two\u2028Three"))
    }

    @Test
    fun `the marks of a wrong encoding go`() {
        assertEquals("Caf - Song", cleanStreamText("Caf\uFFFD - Song"))
    }

    @Test
    fun `nothing, white space and a text with no letter or digit count as no text`() {
        listOf(null, "", "   ", " - ", "-", "...", "\uFFFD\uFFFD", "\u0000").forEach {
            assertNull("[$it]", cleanStreamText(it))
        }
    }

    @Test
    fun `a title of 5,000 characters is cut to 200`() {
        val cut = cleanStreamText("a".repeat(5_000))

        assertEquals(200, cut?.length)
    }

    @Test
    fun `a cut never leaves half a character`() {
        // Each of these is one character made of two halves.
        val cut = cleanStreamText("a" + "\uD83C\uDFB5".repeat(300)).orEmpty()

        assertEquals(199, cut.length)
        assertTrue(cut.last().isLowSurrogate())
    }
}
