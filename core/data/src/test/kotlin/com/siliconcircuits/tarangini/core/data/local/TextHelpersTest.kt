/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextHelpersTest {
    @Test
    fun `a sort key ignores case, outer spaces and two ways to write a letter`() {
        assertEquals("apple", sortKey("  Apple "))
        assertEquals(sortKey("café"), sortKey("café"))
        assertEquals("", sortKey(""))
    }

    @Test
    fun `a sort key keeps text in other scripts`() {
        assertEquals("ఎందరో", sortKey("ఎందరో"))
    }

    @Test
    fun `a search in one column gets lower case words with the column name, so none is an operator`() {
        assertEquals("album:or* album:not* album:near*", FtsQuery.prefixIn("album", "OR \"NOT\" (NEAR)"))
        assertEquals("title:ఎందరో*", FtsQuery.prefixIn("title", "-ఎందరో-"))
        assertNull(FtsQuery.prefixIn("artist", " - & / "))
    }

    @Test
    fun `each word of a search becomes a quoted prefix term`() {
        assertEquals("\"morning*\" \"raga*\"", FtsQuery.prefix("  morning   raga "))
    }

    @Test
    fun `quotes, stars and other signs in a search are dropped`() {
        assertEquals("\"a*\" \"b*\" \"OR*\" \"c*\"", FtsQuery.prefix("a\"b* OR \"\"c"))
        assertEquals("\"rock*\" \"roll*\"", FtsQuery.prefix("rock - (roll): & ^"))
        assertEquals("\"ఎందరో*\"", FtsQuery.prefix("-ఎందరో-"))
    }

    @Test
    fun `a search with no word gives no query`() {
        assertNull(FtsQuery.prefix(""))
        assertNull(FtsQuery.prefix(" \"* \" "))
        assertNull(FtsQuery.prefix(" - & / "))
    }

    @Test
    fun `a very long search is cut to a few short words`() {
        val terms = requireNotNull(FtsQuery.prefix(List(50) { "w".repeat(100) }.joinToString(" "))).split(" ")

        assertEquals(8, terms.size)
        assertEquals("\"" + "w".repeat(40) + "*\"", terms.first())
    }
}
