/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * The keys of the settings store. A key name is what is saved on the phone,
 * so a name is never changed and never used again for another meaning.
 */
internal object SettingsKeys {
    /** The file the settings are saved in. */
    const val FILE_NAME = "settings"

    /** Not used: the app has one dark look. The key stays, as a key name never changes. */
    val THEME_MODE = stringPreferencesKey("theme_mode")

    /** Not used: the app has one dark look. The key stays, as a key name never changes. */
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

    /** Files shorter than this many seconds are left out of the library. */
    val MIN_TRACK_SECONDS = intPreferencesKey("min_track_seconds")

    /** When this is not empty, only these folders are scanned. */
    val INCLUDED_FOLDERS = stringSetPreferencesKey("included_folders")

    /** The order of the list of all songs: the name of a `SongSort`. */
    val SONG_SORT = stringPreferencesKey("song_sort")

    /** Folders a scan skips. */
    val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")

    /** True when the equalizer is on. */
    val EQUALIZER_ON = booleanPreferencesKey("equalizer_on")

    /** The name of the picked `EqualizerPreset`, or no value when the bands were set by hand. */
    val EQUALIZER_PRESET = stringPreferencesKey("equalizer_preset")

    /** The gain of each band in dB, as numbers with a comma between them. */
    val EQUALIZER_GAINS = stringPreferencesKey("equalizer_gains")

    /** The bass boost in percent. */
    val EQUALIZER_BASS = intPreferencesKey("equalizer_bass")

    /** True when a song plays with the preset of its genre. */
    val EQUALIZER_GENRES_ON = booleanPreferencesKey("equalizer_genres_on")

    /** The preset of each genre, each as the preset name, a tab and the genre key. */
    val EQUALIZER_GENRE_PRESETS = stringSetPreferencesKey("equalizer_genre_presets")
}
