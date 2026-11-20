/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import kotlinx.serialization.Serializable

/**
 * The screen of one album. The texts of the top bar travel with the route,
 * so the screen can show them before it has read anything.
 */
@Serializable
data class AlbumRoute(val albumId: Long, val title: String = "", val artist: String = "") {
    internal companion object {
        const val ALBUM_ID = "albumId"
        const val TITLE = "title"
        const val ARTIST = "artist"
    }
}
