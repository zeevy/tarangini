/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

/**
 * Turns text a person typed or spoke into a query for the song search table.
 * The text is not trusted: only letters, digits and their marks are kept,
 * and every word becomes a quoted prefix term, so no character in it can act
 * as a search operator.
 */
internal object FtsQuery {
    private const val MAX_WORDS = 8
    private const val MAX_WORD_LENGTH = 40
    private val separators = Regex("""[^\p{L}\p{N}\p{M}]+""")

    /** A query that matches songs with a word starting with each word of [text], or null when [text] has no word. */
    fun prefix(text: String): String? = text.split(separators)
        .filter { it.isNotEmpty() }
        .take(MAX_WORDS)
        .joinToString(" ") { word -> "\"${word.take(MAX_WORD_LENGTH)}*\"" }
        .ifEmpty { null }
}
