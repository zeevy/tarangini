/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.dao.SongRow
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource
import com.siliconcircuits.tarangini.core.data.mapper.toSong
import com.siliconcircuits.tarangini.core.data.mediastore.localSourceIdOf
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import javax.inject.Inject

/** [SongLookup] on the library tables. */
internal class DefaultSongLookup @Inject constructor(private val library: LibraryDao) : SongLookup {
    override suspend fun byMediaUri(mediaUris: List<String>): Map<String, Song> = mediaUris
        .mapNotNull(::localSourceIdOf)
        .distinct()
        .chunked(MAX_IDS_PER_READ)
        .flatMap { library.songsBySourceIds(SongSource.LOCAL, it) }
        .map(SongRow::toSong)
        .associateBy(Song::mediaUri)

    private companion object {
        /** How many file ids one read takes. SQLite limits the values of one statement. */
        const val MAX_IDS_PER_READ = 500
    }
}
