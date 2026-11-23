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
import com.siliconcircuits.tarangini.core.data.local.dao.FolderDao
import com.siliconcircuits.tarangini.core.data.settings.SettingsKeys
import com.siliconcircuits.tarangini.core.domain.model.Look
import com.siliconcircuits.tarangini.core.domain.model.MAX_MIN_TRACK_SECONDS
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The choices of the user in the one settings store of the app. */
internal class DefaultSettingsRepository @Inject constructor(
    private val settings: DataStore<Preferences>,
    private val folders: FolderDao,
) : SettingsRepository {
    // A settings file that cannot be read gives the first values. It must not end the screen that asks.
    private val saved = settings.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    override fun look(): Flow<Look> = saved
        .map { saved -> Look.entries.firstOrNull { it.name == saved[SettingsKeys.THEME_MODE] } ?: Look.FOLLOW_PHONE }
        .distinctUntilChanged()

    override suspend fun setLook(look: Look) {
        settings.edit { it[SettingsKeys.THEME_MODE] = look.name }
    }

    override fun songSort(): Flow<SongSort> = saved
        .map { saved -> SongSort.entries.firstOrNull { it.name == saved[SettingsKeys.SONG_SORT] } ?: SongSort.TITLE }
        .distinctUntilChanged()

    override suspend fun setSongSort(sort: SongSort) {
        settings.edit { it[SettingsKeys.SONG_SORT] = sort.name }
    }

    override fun scanSettings(): Flow<ScanSettings> = saved
        .map { saved ->
            ScanSettings(
                minTrackSeconds = (saved[SettingsKeys.MIN_TRACK_SECONDS] ?: 0).coerceIn(0, MAX_MIN_TRACK_SECONDS),
                hiddenFolders = saved[SettingsKeys.EXCLUDED_FOLDERS].orEmpty(),
            )
        }
        .distinctUntilChanged()

    override suspend fun setMinTrackSeconds(seconds: Int) {
        settings.edit { it[SettingsKeys.MIN_TRACK_SECONDS] = seconds.coerceIn(0, MAX_MIN_TRACK_SECONDS) }
    }

    override suspend fun setFolderHidden(path: String, hidden: Boolean) {
        settings.edit { saved ->
            val before = saved[SettingsKeys.EXCLUDED_FOLDERS].orEmpty()
            saved[SettingsKeys.EXCLUDED_FOLDERS] = if (hidden) before + path else before - path
        }
    }

    override suspend fun audioFolders(): List<String> = folders.pathsWithSongs()
}
