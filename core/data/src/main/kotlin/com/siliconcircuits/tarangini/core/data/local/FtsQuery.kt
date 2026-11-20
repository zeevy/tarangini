/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

import java.util.Locale

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
    fun prefix(text: String): String? = words(text).joinToString(" ") { "\"$it*\"" }.ifEmpty { null }

    /**
     * The same as [prefix], but the words are looked for only in [column] of
     * the search table (`title`, `artist` or `album`). The column name comes
     * from the code, never from a person.
     *
     * The search table takes no quoted word after a column name. The words
     * are made lower case instead: the operators of the table are written in
     * capitals, so a lower case word is never one, and the table compares
     * without regard to case anyway.
     */
    fun prefixIn(column: String, text: String): String? =
        words(text).joinToString(" ") { "$column:${it.lowercase(Locale.ROOT)}*" }.ifEmpty { null }

    /**
     * The words of [text] that a search uses: at most [MAX_WORDS], each cut
     * to [MAX_WORD_LENGTH]. A piece with no letter and no digit, such as a
     * mark that stands alone, is no word: the table would find nothing for it.
     */
    fun words(text: String): List<String> = text.split(separators)
        .filter { piece -> piece.any(Char::isLetterOrDigit) }
        .take(MAX_WORDS)
        .map { it.take(MAX_WORD_LENGTH) }
}
