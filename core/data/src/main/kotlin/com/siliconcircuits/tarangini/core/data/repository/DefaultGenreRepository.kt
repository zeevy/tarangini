/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.GenreDao
import com.siliconcircuits.tarangini.core.data.local.dao.GenreRow
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.dao.SongGenreKey
import com.siliconcircuits.tarangini.core.data.local.dao.SongRow
import com.siliconcircuits.tarangini.core.data.mapper.toSong
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.GenreRepository
import javax.inject.Inject

/** The genres from the database of the app. Every list is one query with a limit. */
internal class DefaultGenreRepository @Inject constructor(
    private val genres: GenreDao,
    private val library: LibraryDao,
) : GenreRepository {
    override suspend fun genres(page: Page): List<Genre> = genres.genres(page.limit, page.offset).map { it.toGenre() }

    override suspend fun genre(genreId: Long): Genre? = genres.genre(genreId)?.toGenre()

    override suspend fun songsOfGenre(genreId: Long, page: Page): List<Song> =
        genres.songsOfGenre(genreId, page.limit, page.offset).map(SongRow::toSong)

    override suspend fun genreKeysOf(songIds: List<Long>): Map<Long, List<String>> = songIds
        .distinct()
        .chunked(MAX_IDS_PER_READ)
        .flatMap { library.genreKeysOfSongs(it) }
        .groupBy(SongGenreKey::songId, SongGenreKey::nameKey)

    private companion object {
        /** How many songs one read takes. SQLite limits the values of one statement. */
        const val MAX_IDS_PER_READ = 500
    }
}

private fun GenreRow.toGenre() = Genre(id, name, songCount, nameKey)
