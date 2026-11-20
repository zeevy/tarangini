/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import com.siliconcircuits.tarangini.core.domain.model.LetterCount
import com.siliconcircuits.tarangini.core.domain.model.Page

/**
 * The rows of a list by name that the car shows under one letter node.
 *
 * @property letter the code point of the letter, or [MediaId.BEFORE_LETTERS]
 * @property offset the place of its first row in the whole list
 */
internal data class LetterGroup(val letter: Int, val offset: Int, val count: Int) {
    /** What the node shows: the letter in upper case, or "#" for what is before the letters. */
    fun label(): String = if (letter == MediaId.BEFORE_LETTERS) "#" else String(Character.toChars(letter)).uppercase()

    /** The read that gives its rows, or null when it has none. Rows after the first [Page.MAX_LIMIT] are left out. */
    fun page(): Page? = if (count > 0) Page(limit = count.coerceAtMost(Page.MAX_LIMIT), offset = offset) else null
}

/**
 * The letter nodes of a list. The names that sort before the first letter
 * (digits, signs, an empty name) share the one node "#", so a list does
 * not start with a row for every digit and sign.
 */
internal fun List<LetterCount>.toGroups(): List<LetterGroup> {
    val firstLetter = indexOfFirst { Character.isLetter(it.codePoint()) }.let { if (it < 0) size else it }
    val before = take(firstLetter).sumOf { it.count }
    val head = if (before > 0) listOf(LetterGroup(MediaId.BEFORE_LETTERS, offset = 0, count = before)) else emptyList()
    var offset = before
    return head + drop(firstLetter).map { letter ->
        LetterGroup(letter.codePoint(), offset, letter.count).also { offset += letter.count }
    }
}

private fun LetterCount.codePoint(): Int = if (letter.isEmpty()) MediaId.BEFORE_LETTERS else letter.codePointAt(0)
