/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingScreen

/** Opens the now playing screen. A second tap while it opens does nothing. */
fun NavController.navigateToNowPlaying() {
    navigate(NowPlayingRoute) { launchSingleTop = true }
}

fun NavGraphBuilder.nowPlayingScreen(onBackClick: () -> Unit) {
    composable<NowPlayingRoute> { entry ->
        // A second tap that comes while the screen already closes is dropped.
        NowPlayingScreen(
            onBackClick = { if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) onBackClick() },
        )
    }
}
