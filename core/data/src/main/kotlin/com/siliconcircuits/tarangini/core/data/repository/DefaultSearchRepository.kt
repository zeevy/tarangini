/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.FtsQuery
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.sortKey
import com.siliconcircuits.tarangini.core.data.mapper.toAlbum
import com.siliconcircuits.tarangini.core.data.mapper.toArtist
import com.siliconcircuits.tarangini.core.data.mapper.toSong
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.domain.repository.SearchRepository
import javax.inject.Inject

/** Search through the full text table of the songs. */
internal class DefaultSearchRepository @Inject constructor(private val library: LibraryDao) : SearchRepository {
    override suspend fun search(text: String, limit: Int): SearchResults {
        require(limit in 1..SearchRepository.MAX_LIMIT) { "A search gives 1 to ${SearchRepository.MAX_LIMIT} rows" }
        // The text can come from any app that may talk to the player, so its length is not trusted.
        val asked = text.take(MAX_TEXT_LENGTH)
        val anywhere = FtsQuery.prefix(asked) ?: return SearchResults()
        // The start of a name is compared without the signs the person typed between the words.
        val start = sortKey(FtsQuery.words(asked).joinToString(" "))
        return SearchResults(
            songs = library.searchSongs(anywhere, inColumn("title", asked), start, limit).map { it.toSong() },
            albums = library.searchAlbums(inColumn("album", asked), start, limit).map { it.toAlbum() },
            artists = library.searchArtists(inColumn("artist", asked), start, limit).map { it.toArtist() },
        )
    }

    private fun inColumn(column: String, text: String): String = checkNotNull(FtsQuery.prefixIn(column, text))

    private companion object {
        const val MAX_TEXT_LENGTH = 256
    }
}
