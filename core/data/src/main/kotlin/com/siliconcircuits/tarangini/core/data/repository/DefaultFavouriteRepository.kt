/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.FavouriteDao
import com.siliconcircuits.tarangini.core.data.mapper.toSong
import com.siliconcircuits.tarangini.core.data.mediastore.TimeSource
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Favourite songs from the database of the app. */
internal class DefaultFavouriteRepository @Inject constructor(
    private val favourites: FavouriteDao,
    private val time: TimeSource,
) : FavouriteRepository {
    override fun songIds(): Flow<Set<Long>> = favourites.favouriteSongIds().map { it.toSet() }.distinctUntilChanged()

    override suspend fun setFavourite(songId: Long, favourite: Boolean) {
        if (favourite) favourites.addKnownSong(songId, time.nowMs()) else favourites.removeSong(songId)
    }

    override suspend fun songs(page: Page): List<Song> =
        favourites.favouriteSongs(page.limit, page.offset).map { it.toSong() }
}
