/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.SongSort
import kotlinx.coroutines.flow.Flow

/** The choices of the user that the app keeps between starts. */
interface SettingsRepository {
    /** The order of the list of all songs. It is [SongSort.TITLE] until the user chooses another. */
    fun songSort(): Flow<SongSort>

    /** Saves the order of the list of all songs. */
    suspend fun setSongSort(sort: SongSort)
}
