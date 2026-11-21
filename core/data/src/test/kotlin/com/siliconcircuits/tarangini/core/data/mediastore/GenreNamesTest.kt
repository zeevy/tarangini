/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import org.junit.Assert.assertEquals
import org.junit.Test

class GenreNamesTest {
    @Test
    fun `a tag names one genre or several`() {
        assertEquals(listOf("Rock"), genreNames("Rock"))
        assertEquals(listOf("Rock", "Pop", "Folk"), genreNames(" Rock ;Pop, Folk"))
        assertEquals(listOf("Rock", "Pop"), genreNames("Rock\u0000Pop"))
        assertEquals(listOf("R&B/Soul"), genreNames("R&B/Soul"))
    }

    @Test
    fun `the same genre twice counts once, and no more than five are kept`() {
        assertEquals(listOf("Rock"), genreNames("Rock; rock ;ROCK"))
        assertEquals(5, genreNames("a;b;c;d;e;f;g").size)
        assertEquals(100, genreNames("x".repeat(300)).single().length)
    }

    @Test
    fun `no tag, or a tag with no name in it, is the genre with no name`() {
        assertEquals(listOf(""), genreNames(null))
        assertEquals(listOf(""), genreNames(" ; , "))
    }
}
