/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.siliconcircuits.tarangini.core.data.settings.SettingsKeys
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.settings() =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("settings.preferences_pb") }

    @Test
    fun `the order of the songs is title until one is chosen, and the choice is kept`() = runTest {
        val settings = settings()
        assertEquals(SongSort.TITLE, DefaultSettingsRepository(settings).songSort().first())

        DefaultSettingsRepository(settings).setSongSort(SongSort.DATE_ADDED)

        assertEquals(SongSort.DATE_ADDED, DefaultSettingsRepository(settings).songSort().first())
    }

    @Test
    fun `a settings file that cannot be read gives title and no error`() = runTest {
        val broken = folder.newFile("broken.preferences_pb").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val settings = PreferenceDataStoreFactory.create(scope = backgroundScope) { broken }

        assertEquals(SongSort.TITLE, DefaultSettingsRepository(settings).songSort().first())
    }

    @Test
    fun `a saved order that the app does not know reads as title`() = runTest {
        val settings = settings()
        settings.edit { it[SettingsKeys.SONG_SORT] = "BY_MOOD" }

        assertEquals(SongSort.TITLE, DefaultSettingsRepository(settings).songSort().first())
    }
}
