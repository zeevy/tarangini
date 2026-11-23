/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Look
import com.siliconcircuits.tarangini.core.domain.model.MAX_MIN_TRACK_SECONDS
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import kotlinx.coroutines.flow.Flow

/** The choices of the user that the app keeps between starts. */
interface SettingsRepository {
    /** The colours of the app. It is [Look.FOLLOW_PHONE] until the person picks another. */
    fun look(): Flow<Look>

    /** Saves the colours of the app. The screens change at once. */
    suspend fun setLook(look: Look)

    /** The order of the list of all songs. It is [SongSort.TITLE] until the user chooses another. */
    fun songSort(): Flow<SongSort>

    /** Saves the order of the list of all songs. */
    suspend fun setSongSort(sort: SongSort)

    /** What is left out of the library. Nothing is left out until the person chooses so. */
    fun scanSettings(): Flow<ScanSettings>

    /** Saves the shortest track that is kept, from 0 to [MAX_MIN_TRACK_SECONDS] seconds. The next scan uses it. */
    suspend fun setMinTrackSeconds(seconds: Int)

    /** Hides a folder from the library or shows it again. The next scan uses it. */
    suspend fun setFolderHidden(path: String, hidden: Boolean)

    /** Every folder of the phone that holds audio the library knows, hidden ones too, by path. */
    suspend fun audioFolders(): List<String>
}
