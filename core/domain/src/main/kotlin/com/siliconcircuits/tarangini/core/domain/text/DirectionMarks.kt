/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.text

/**
 * True for a character that is not seen and changes the direction of the text around it, and for
 * the byte order mark. Such a character in a text from a file can make a line read as something
 * else, so it is taken out before the text is shown.
 *
 * The joiners (U+200C and U+200D) are not among them. They are not seen either, but the Indian
 * scripts and Persian need them to join their letters the right way.
 */
fun Char.isDirectionMark(): Boolean = this in MARKS || this in EMBEDDINGS || this in ISOLATES

private const val MARKS = "\u200E\u200F\u061C\uFEFF"
private val EMBEDDINGS = '\u202A'..'\u202E'
private val ISOLATES = '\u2066'..'\u2069'
