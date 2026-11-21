/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song

/**
 * The genres of the library. A song with more than one genre is under each of them, and a song
 * with no genre tag is under the genre with the empty name. [LibraryRepository.changes] says when
 * to read again.
 */
interface GenreRepository {
    /** The genres that have a song to play, by name, with the genre with no name last. */
    suspend fun genres(page: Page): List<Genre>

    /** One genre, or null when it is gone or has no song to play. */
    suspend fun genre(genreId: Long): Genre?

    /** The songs of a genre by title. */
    suspend fun songsOfGenre(genreId: Long, page: Page): List<Song>
}
