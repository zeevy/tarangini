/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.feature.library.permission

import android.Manifest
import android.os.Build

/**
 * The permission that lets the app list audio files on a phone with Android API level [sdk].
 * Android 13 has a permission for audio alone. Before that there is only the one for all files.
 */
internal fun audioPermissionFor(sdk: Int): String = if (sdk >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_AUDIO
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE
}
