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
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.audio.MAX_BAND_GAIN_DB
import com.siliconcircuits.tarangini.core.domain.repository.EqualizerRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The equalizer in the one settings store of the app. */
internal class DefaultEqualizerRepository @Inject constructor(private val settings: DataStore<Preferences>) :
    EqualizerRepository {
    override fun settings(): Flow<EqualizerSettings> = settings.data
        // A settings file that cannot be read gives the equalizer off. It must not stop the playback.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::read)
        .distinctUntilChanged()

    override suspend fun save(settings: EqualizerSettings) {
        this.settings.edit { saved ->
            saved[SettingsKeys.EQUALIZER_ON] = settings.isOn
            val preset = settings.preset
            if (preset ==
                null
            ) {
                saved.remove(SettingsKeys.EQUALIZER_PRESET)
            } else {
                saved[SettingsKeys.EQUALIZER_PRESET] =
                    preset.name
            }
            saved[SettingsKeys.EQUALIZER_GAINS] = settings.ownGains.joinToString(",")
            saved[SettingsKeys.EQUALIZER_BASS] = settings.bassBoost.coerceIn(0, EqualizerSettings.MAX_BASS_PERCENT)
            saved[SettingsKeys.EQUALIZER_GENRES_ON] = settings.genresOn
            saved[SettingsKeys.EQUALIZER_GENRE_PRESETS] =
                settings.genrePresets.map { (key, preset) -> preset.name + GENRE_SEPARATOR + key }.toSet()
        }
    }

    /** The settings in [saved]. A value that is broken or out of range is read as its default or its limit. */
    private fun read(saved: Preferences): EqualizerSettings {
        val gains = saved[SettingsKeys.EQUALIZER_GAINS].orEmpty().split(',').mapNotNull { text ->
            text.toFloatOrNull()?.takeIf(Float::isFinite)?.coerceIn(-MAX_BAND_GAIN_DB, MAX_BAND_GAIN_DB)
        }
        val presetName = saved[SettingsKeys.EQUALIZER_PRESET]
        // Bands set by hand have no preset. With no own gains saved either, the first value is flat.
        val preset = EqualizerPreset.entries.firstOrNull { it.name == presetName }
            ?: EqualizerPreset.FLAT.takeIf { gains.isEmpty() }
        return EqualizerSettings(
            isOn = saved[SettingsKeys.EQUALIZER_ON] == true,
            preset = preset,
            ownGains = gains,
            bassBoost = (saved[SettingsKeys.EQUALIZER_BASS] ?: 0).coerceIn(0, EqualizerSettings.MAX_BASS_PERCENT),
            genresOn = saved[SettingsKeys.EQUALIZER_GENRES_ON] == true,
            genrePresets = saved[SettingsKeys.EQUALIZER_GENRE_PRESETS].orEmpty().mapNotNull(::genrePreset).toMap(),
        )
    }

    /** One saved genre preset; a broken one is left out. */
    private fun genrePreset(text: String): Pair<String, EqualizerPreset>? {
        val name = text.substringBefore(GENRE_SEPARATOR)
        val key = text.substringAfter(GENRE_SEPARATOR, missingDelimiterValue = "")
        val preset = EqualizerPreset.entries.firstOrNull { it.name == name }
        return if (preset == null || key.isEmpty()) null else key to preset
    }

    private companion object {
        /** A preset name has no tab, so the first tab ends it. */
        const val GENRE_SEPARATOR = '\t'
    }
}
