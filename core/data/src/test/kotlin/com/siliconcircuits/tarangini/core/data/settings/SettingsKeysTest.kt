/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsKeysTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `every setting is written and read back with its own type`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            folder.newFile("settings.preferences_pb")
        }
        assertNull(store.data.first()[SettingsKeys.THEME_MODE])

        store.edit {
            it[SettingsKeys.THEME_MODE] = "dark"
            it[SettingsKeys.DYNAMIC_COLOR] = false
            it[SettingsKeys.MIN_TRACK_SECONDS] = 30
            it[SettingsKeys.INCLUDED_FOLDERS] = setOf("Music")
            it[SettingsKeys.EXCLUDED_FOLDERS] = setOf("WhatsApp", "Recordings")
        }

        val saved = store.data.first()
        assertEquals("dark", saved[SettingsKeys.THEME_MODE])
        assertEquals(false, saved[SettingsKeys.DYNAMIC_COLOR])
        assertEquals(30, saved[SettingsKeys.MIN_TRACK_SECONDS])
        assertEquals(setOf("Music"), saved[SettingsKeys.INCLUDED_FOLDERS])
        assertEquals(setOf("WhatsApp", "Recordings"), saved[SettingsKeys.EXCLUDED_FOLDERS])
    }

    @Test
    fun `the saved names of the keys do not change`() {
        val names = listOf(
            SettingsKeys.THEME_MODE,
            SettingsKeys.DYNAMIC_COLOR,
            SettingsKeys.MIN_TRACK_SECONDS,
            SettingsKeys.INCLUDED_FOLDERS,
            SettingsKeys.EXCLUDED_FOLDERS,
            SettingsKeys.SONG_SORT,
        ).map { it.name }

        assertEquals(
            listOf(
                "theme_mode",
                "dynamic_color",
                "min_track_seconds",
                "included_folders",
                "excluded_folders",
                "song_sort",
            ),
            names,
        )
        assertEquals("settings", SettingsKeys.FILE_NAME)
    }
}
