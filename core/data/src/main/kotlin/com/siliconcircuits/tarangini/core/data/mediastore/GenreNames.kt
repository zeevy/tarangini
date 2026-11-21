/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.data.local.sortKey

/**
 * The genres a genre tag names. One tag can hold several, divided by ";", "," or the zero
 * character of the newer tag format. "/" does not divide: "R&B/Soul" is one genre.
 *
 * A tag with no name gives the one genre with the empty name, which the screens show as "Unknown".
 * So every song has a genre.
 */
internal fun genreNames(tag: String?): List<String> = tag.orEmpty()
    .split(GENRE_DIVIDER, ',', '\u0000')
    .map { it.trim().take(MAX_GENRE_NAME) }
    .filter { it.isNotEmpty() }
    .distinctBy(::sortKey)
    .take(MAX_GENRES_PER_SONG)
    .ifEmpty { listOf("") }

/** The sign that divides the genres of one tag. */
internal const val GENRE_DIVIDER = ';'

private const val MAX_GENRE_NAME = 100
private const val MAX_GENRES_PER_SONG = 5
