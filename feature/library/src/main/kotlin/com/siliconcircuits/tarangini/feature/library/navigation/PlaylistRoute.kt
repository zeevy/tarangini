/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import androidx.navigation.NavController
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import kotlinx.serialization.Serializable

/** The screen of one playlist. The name is shown at once; the screen reads it again after a rename. */
@Serializable
data class PlaylistRoute(val playlistId: Long, val name: String = "") {
    internal companion object {
        const val PLAYLIST_ID = "playlistId"
        const val NAME = "name"
    }
}

/** Opens the screen of a playlist from outside the Library tab. The name is cut, as a route has a size limit. */
fun NavController.navigateToPlaylist(playlist: Playlist) {
    navigate(PlaylistRoute(playlist.id, playlist.name.take(MAX_NAME))) { launchSingleTop = true }
}

private const val MAX_NAME = 200
