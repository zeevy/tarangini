/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.player.equalizer.EqualizerScreen
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingScreen
import com.siliconcircuits.tarangini.feature.player.queue.QueueScreen

/** Opens the now playing screen. A second tap while it opens does nothing. */
fun NavController.navigateToNowPlaying() {
    navigate(NowPlayingRoute) { launchSingleTop = true }
}

/**
 * True on the now playing screen and on the queue. Both show the current item with its controls,
 * so the mini player would be a second copy there.
 */
fun NavDestination.isPlayerScreen(): Boolean = hasRoute<NowPlayingRoute>() || hasRoute<QueueRoute>()

/** The now playing screen, and the queue and the equalizer that open from it. */
fun NavGraphBuilder.playerScreens(navController: NavController) {
    composable<NowPlayingRoute> { entry ->
        // A second tap that comes while the screen already changes is dropped.
        NowPlayingScreen(
            onBackClick = { if (entry.isShown()) navController.popBackStack() },
            onOpenQueue = { if (entry.isShown()) navController.navigate(QueueRoute) { launchSingleTop = true } },
            onOpenEqualizer = {
                if (entry.isShown()) navController.navigate(EqualizerRoute) { launchSingleTop = true }
            },
        )
    }
    composable<EqualizerRoute> { entry ->
        EqualizerScreen(onBackClick = { if (entry.isShown()) navController.popBackStack() })
    }
    composable<QueueRoute> { entry ->
        QueueScreen(onBackClick = { if (entry.isShown()) navController.popBackStack() })
    }
}

private fun NavBackStackEntry.isShown() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
