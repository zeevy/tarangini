/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local

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
    fun `each word of a search becomes a quoted prefix term`() {
        assertEquals("\"morning*\" \"raga*\"", FtsQuery.prefix("  morning   raga "))
    }

    @Test
    fun `quotes and stars in a search are dropped`() {
        assertEquals("\"a*\" \"b*\" \"OR*\" \"c*\"", FtsQuery.prefix("a\"b* OR \"\"c"))
    }

    @Test
    fun `a search with no word gives no query`() {
        assertNull(FtsQuery.prefix(""))
        assertNull(FtsQuery.prefix(" \"* \" "))
    }

    @Test
    fun `a very long search is cut to a few short words`() {
        val terms = requireNotNull(FtsQuery.prefix(List(50) { "w".repeat(100) }.joinToString(" "))).split(" ")

        assertEquals(8, terms.size)
        assertEquals("\"" + "w".repeat(40) + "*\"", terms.first())
    }
}
