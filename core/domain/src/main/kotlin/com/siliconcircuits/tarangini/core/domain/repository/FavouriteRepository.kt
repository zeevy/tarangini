/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** The songs the person marked as favourites. The phone and the car read the same set. */
interface FavouriteRepository {
    /** The ids of all favourite songs. A new set comes after every change, so every list can mark its rows. */
    fun songIds(): Flow<Set<Long>>

    /** Marks a song as a favourite or takes the mark away. A song the library does not know is left alone. */
    suspend fun setFavourite(songId: Long, favourite: Boolean)

    /** The favourite songs whose file is there, the one marked last first. */
    suspend fun songs(page: Page): List<Song>
}
