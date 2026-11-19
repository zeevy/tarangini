/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local

/**
 * Turns text a person typed or spoke into a query for the song search table.
 * The text is not trusted: every word becomes a quoted prefix term, so no
 * character in it can act as a search operator.
 */
internal object FtsQuery {
    private const val MAX_WORDS = 8
    private const val MAX_WORD_LENGTH = 40
    private val separators = Regex("""[\s"*]+""")

    /** A query that matches songs with a word starting with each word of [text], or null when [text] has no word. */
    fun prefix(text: String): String? = text.split(separators)
        .filter { it.isNotEmpty() }
        .take(MAX_WORDS)
        .joinToString(" ") { word -> "\"${word.take(MAX_WORD_LENGTH)}*\"" }
        .ifEmpty { null }
}
