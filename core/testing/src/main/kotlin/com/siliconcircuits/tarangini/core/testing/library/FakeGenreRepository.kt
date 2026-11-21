/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.GenreRepository

/** Genres for tests: a test puts the songs of each genre name into [songsByGenre]. */
class FakeGenreRepository : GenreRepository {
    /** The songs of each genre by its name, in the order a list shows them. The id of a genre is its place plus 1. */
    var songsByGenre: Map<String, List<Song>> = emptyMap()

    private fun all(): List<Genre> =
        songsByGenre.entries.mapIndexed { index, (name, songs) -> Genre(index + 1L, name, songs.size) }

    override suspend fun genres(page: Page): List<Genre> = all().drop(page.offset).take(page.limit)

    override suspend fun genre(genreId: Long): Genre? = all().firstOrNull { it.id == genreId }

    override suspend fun songsOfGenre(genreId: Long, page: Page): List<Song> =
        songsByGenre[genre(genreId)?.name].orEmpty().drop(page.offset).take(page.limit)
}
