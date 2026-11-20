/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.LetterCount

/**
 * The first letters of the three lists of the library that are sorted by
 * name. The car has no paging, so it shows a long list as one short list
 * per letter.
 *
 * Each answer is in the order of the list itself, and counts the same rows
 * as [LibraryRepository] gives. So the counts added up give the place of a
 * letter in the list, and its rows are read there with a page.
 */
interface LibraryLetters {
    /** The first letters of the song titles, in the order of the songs by title. */
    suspend fun ofSongs(): List<LetterCount>

    /** The first letters of the album titles, in the order of the albums. */
    suspend fun ofAlbums(): List<LetterCount>

    /** The first letters of the artist names, in the order of the artists. */
    suspend fun ofArtists(): List<LetterCount>
}
