/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.data.settings.SettingsKeys
import com.siliconcircuits.tarangini.core.domain.model.Look
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = testDatabase()

    @After
    fun closeDatabase() = database.close()

    private fun TestScope.store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("settings.preferences_pb") }

    @Test
    fun `nothing is left out at first, and what is chosen is read back by a new repository`() = runTest {
        val store = store()
        val settings = DefaultSettingsRepository(store, database.folderDao())
        val first = settings.scanSettings().first()

        settings.setMinTrackSeconds(30)
        settings.setFolderHidden("external_primary/WhatsApp", true)
        settings.setFolderHidden("external_primary/Calls", true)
        settings.setFolderHidden("external_primary/Calls", false)

        assertEquals(ScanSettings(), first)
        assertEquals(
            ScanSettings(30, setOf("external_primary/WhatsApp")),
            DefaultSettingsRepository(store, database.folderDao()).scanSettings().first(),
        )
    }

    @Test
    fun `the look follows the phone at first, and a pick is read back by a new repository`() = runTest {
        val store = store()
        val first = DefaultSettingsRepository(store, database.folderDao()).look().first()

        DefaultSettingsRepository(store, database.folderDao()).setLook(Look.DARK)

        assertEquals(Look.FOLLOW_PHONE, first)
        assertEquals(Look.DARK, DefaultSettingsRepository(store, database.folderDao()).look().first())
    }

    @Test
    fun `the visual shows bars at first, and a pick is read back by a new repository`() = runTest {
        val store = store()
        val first = DefaultSettingsRepository(store, database.folderDao()).visualMode().first()

        DefaultSettingsRepository(store, database.folderDao()).setVisualMode(VisualMode.WAVE)

        assertEquals(VisualMode.BARS, first)
        assertEquals(VisualMode.WAVE, DefaultSettingsRepository(store, database.folderDao()).visualMode().first())
    }

    @Test
    fun `an old theme value that is not a look counts as no value`() = runTest {
        val store = store()
        store.edit { it[SettingsKeys.THEME_MODE] = "SYSTEM" }

        assertEquals(Look.FOLLOW_PHONE, DefaultSettingsRepository(store, database.folderDao()).look().first())
    }

    @Test
    fun `a track length outside the limits is brought inside them`() = runTest {
        val settings = DefaultSettingsRepository(store(), database.folderDao())

        settings.setMinTrackSeconds(-5)
        val low = settings.scanSettings().first().minTrackSeconds
        settings.setMinTrackSeconds(100_000)

        assertEquals(0 to 600, low to settings.scanSettings().first().minTrackSeconds)
    }

    @Test
    fun `the folders with songs are listed once each, by path`() = runTest {
        val settings = DefaultSettingsRepository(store(), database.folderDao())
        val music = database.libraryDao().saveFolder("external_primary/Music", "Music", null)
        val ragas = database.libraryDao().saveFolder("external_primary/Music/Ragas", "Ragas", music)
        database.libraryDao().saveFolder("external_primary/Empty", "Empty", null)
        database.addSong("One", sourceId = "1", folderId = ragas)
        database.addSong("Two", sourceId = "2", folderId = ragas)
        database.addSong("Three", sourceId = "3", folderId = music)

        assertEquals(listOf("external_primary/Music", "external_primary/Music/Ragas"), settings.audioFolders())
    }
}
