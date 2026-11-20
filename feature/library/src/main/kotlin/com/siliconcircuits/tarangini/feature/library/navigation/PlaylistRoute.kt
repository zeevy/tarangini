/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import kotlinx.serialization.Serializable

/** The screen of one playlist. The name is shown at once; the screen reads it again after a rename. */
@Serializable
data class PlaylistRoute(val playlistId: Long, val name: String = "") {
    internal companion object {
        const val PLAYLIST_ID = "playlistId"
        const val NAME = "name"
    }
}
