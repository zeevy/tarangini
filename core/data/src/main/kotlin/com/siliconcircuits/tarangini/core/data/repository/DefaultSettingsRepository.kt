/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.siliconcircuits.tarangini.core.data.settings.SettingsKeys
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The choices of the user in the one settings store of the app. */
internal class DefaultSettingsRepository @Inject constructor(private val settings: DataStore<Preferences>) :
    SettingsRepository {
    override fun songSort(): Flow<SongSort> = settings.data
        // A settings file that cannot be read gives the first order. It must not end the screen that asks.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { saved -> SongSort.entries.firstOrNull { it.name == saved[SettingsKeys.SONG_SORT] } ?: SongSort.TITLE }
        .distinctUntilChanged()

    override suspend fun setSongSort(sort: SongSort) {
        settings.edit { it[SettingsKeys.SONG_SORT] = sort.name }
    }
}
