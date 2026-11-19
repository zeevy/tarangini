/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
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
