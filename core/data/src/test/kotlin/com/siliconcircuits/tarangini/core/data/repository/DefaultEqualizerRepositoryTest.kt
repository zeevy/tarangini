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
import com.siliconcircuits.tarangini.core.data.settings.SettingsKeys
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.audio.VolumeLevelling
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultEqualizerRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("settings.preferences_pb") }

    @Test
    fun `the equalizer is off and flat at first`() = runTest {
        val equalizer = DefaultEqualizerRepository(store())

        assertEquals(EqualizerSettings(), equalizer.settings().first())
    }

    @Test
    fun `a preset and own bands are read back as they were saved`() = runTest {
        val equalizer = DefaultEqualizerRepository(store())
        val carnatic = EqualizerSettings(isOn = true, preset = EqualizerPreset.CARNATIC, bassBoost = 40)
        val own = EqualizerSettings(isOn = true, preset = null, ownGains = listOf(3f, -2.5f, 0f, 1f, 12f))

        equalizer.save(carnatic)
        assertEquals(carnatic, equalizer.settings().first())
        equalizer.save(own)
        assertEquals(own, equalizer.settings().first())
    }

    @Test
    fun `the preset of each genre is read back, also a genre key with a tab or a space`() = runTest {
        val equalizer = DefaultEqualizerRepository(store())
        val genres = EqualizerSettings(
            isOn = true,
            genresOn = true,
            genrePresets = mapOf("carnatic" to EqualizerPreset.CARNATIC, "hip hop\tlive" to EqualizerPreset.BASS),
        )

        equalizer.save(genres)

        assertEquals(genres, equalizer.settings().first())
    }

    @Test
    fun `the volume levelling is read back, and a broken name is off`() = runTest {
        val store = store()
        val equalizer = DefaultEqualizerRepository(store)

        equalizer.save(EqualizerSettings(levelling = VolumeLevelling.ALBUM))
        assertEquals(VolumeLevelling.ALBUM, equalizer.settings().first().levelling)

        store.edit { it[SettingsKeys.EQUALIZER_LEVELLING] = "LOUD" }
        assertEquals(VolumeLevelling.OFF, equalizer.settings().first().levelling)
    }

    @Test
    fun `a broken genre preset is left out`() = runTest {
        val store = store()
        store.edit { saved ->
            saved[SettingsKeys.EQUALIZER_GENRE_PRESETS] =
                setOf("LOUD\trock", "BASS", "BASS\t", "VOCAL\tpop")
        }

        val read = DefaultEqualizerRepository(store).settings().first()

        assertEquals(mapOf("pop" to EqualizerPreset.VOCAL), read.genrePresets)
    }

    @Test
    fun `broken and out of range values are read as their limits`() = runTest {
        val store = store()
        store.edit { saved ->
            saved[SettingsKeys.EQUALIZER_ON] = true
            saved[SettingsKeys.EQUALIZER_PRESET] = "LOUD"
            saved[SettingsKeys.EQUALIZER_GAINS] = "40,-99,x,NaN,2"
            saved[SettingsKeys.EQUALIZER_BASS] = 500
        }

        val read = DefaultEqualizerRepository(store).settings().first()

        assertEquals(EqualizerSettings(true, preset = null, ownGains = listOf(12f, -12f, 2f), bassBoost = 100), read)
    }
}
