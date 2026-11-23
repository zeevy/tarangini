/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingScreen
import com.siliconcircuits.tarangini.feature.player.nowplaying.PlayerTargets

/**
 * True on the player tab. It shows the current item with its controls, so the mini player would be
 * a second copy there.
 */
fun NavDestination.isPlayerScreen(): Boolean = hasRoute<NowPlayingRoute>()

/**
 * The player tab. The app hands in [player], which shows [PlayerTab]; a test of the app shell
 * hands in a stand-in, as the real screen needs Hilt. [targets] lead to the screens of other features.
 */
fun NavGraphBuilder.playerScreen(targets: PlayerTargets, player: @Composable (PlayerTargets) -> Unit) {
    composable<NowPlayingRoute> { player(targets) }
}

/** The player: the tab itself, and the pane beside the other tabs on a wide window. */
@Composable
fun PlayerTab(targets: PlayerTargets, modifier: Modifier = Modifier) {
    NowPlayingScreen(targets, modifier)
}
