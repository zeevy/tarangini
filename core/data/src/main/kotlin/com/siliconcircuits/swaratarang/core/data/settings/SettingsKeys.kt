/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.settings

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

    /** "system", "light" or "dark". */
    val THEME_MODE = stringPreferencesKey("theme_mode")

    /** True for colours from the wallpaper, false for the brand colours. */
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

    /** Files shorter than this many seconds are left out of the library. */
    val MIN_TRACK_SECONDS = intPreferencesKey("min_track_seconds")

    /** When this is not empty, only these folders are scanned. */
    val INCLUDED_FOLDERS = stringSetPreferencesKey("included_folders")

    /** Folders a scan skips. */
    val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
}
