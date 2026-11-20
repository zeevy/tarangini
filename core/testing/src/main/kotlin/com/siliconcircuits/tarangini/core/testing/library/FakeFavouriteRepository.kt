/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Favourites kept in memory. A test sets [known] to the songs of its library. */
class FakeFavouriteRepository : FavouriteRepository {
    /** The songs that can be shown. A favourite that is not among them counts as a song whose file is gone. */
    var known: List<Song> = emptyList()

    /** The ids of the favourites, the one marked first at the start. */
    val ids = MutableStateFlow<List<Long>>(emptyList())

    override fun songIds(): Flow<Set<Long>> = ids.map { it.toSet() }

    override suspend fun setFavourite(songId: Long, favourite: Boolean) {
        ids.value = if (favourite) (ids.value - songId) + songId else ids.value - songId
    }

    override suspend fun songs(page: Page): List<Song> =
        ids.value.reversed().mapNotNull { id -> known.firstOrNull { it.id == id } }.drop(page.offset).take(page.limit)
}
