/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import androidx.navigation.NavController

/** Opens the screen of the album [id] from a screen that knows only its id and names, such as the player. */
fun NavController.navigateToAlbum(id: Long, title: String, artist: String) {
    navigate(AlbumRoute(id, title.take(MAX_TITLE), artist.take(MAX_TITLE))) { launchSingleTop = true }
}
