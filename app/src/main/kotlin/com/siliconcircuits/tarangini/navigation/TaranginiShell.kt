/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.siliconcircuits.tarangini.feature.player.navigation.isPlayerScreen
import com.siliconcircuits.tarangini.feature.player.nowplaying.PlayerTargets

/**
 * The frame every screen sits in: the screens, then the [miniPlayer] slot,
 * then the bottom bar. The bottom bar shows only on the tabs. Each screen
 * draws its own top bar. The slot is part of the bottom bar area, so a list
 * ends above it and no row is covered.
 *
 * On a wide window the tabs move to a rail at the left, and on a tablet the
 * [player] stands as a pane at the right of the other tab screens in place of the
 * mini player. Folding or turning the device keeps the screen, its scroll place and
 * the playback: only the layout around the screens changes.
 *
 * @param miniPlayer the bar with the current item. It gets the way to open the player tab, and the
 *   modifier it must put on itself
 * @param player the player, as the first tab and as the pane, with the places it leads to
 */
// The player slot shows as the first tab or as the pane, never both at once: the pane is hidden on
// the player tab. Each is a screen of its own with its own state, so the slot is not one moved thing.
@Suppress("ContentSlotReused")
@Composable
internal fun TaranginiShell(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    miniPlayer: @Composable (onOpen: () -> Unit, modifier: Modifier) -> Unit = { _, _ -> },
    player: @Composable (PlayerTargets, Modifier) -> Unit = { _, _ -> },
    settings: @Composable (onOpenLicences: () -> Unit) -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = destination?.topLevelDestination()
    val targets = remember(navController) { playerTargets(navController) }

    BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        val layout = ShellLayout.of(maxWidth, maxHeight)
        // The player tab is the player itself, so it needs no pane beside it.
        val withPane = layout == ShellLayout.EXPANDED && currentTab != null && currentTab != TopLevelDestination.PLAYER
        // The now playing screen shows the same item with all its controls, so the bar would be a second copy.
        val showMiniPlayer = destination?.isPlayerScreen() != true && !withPane
        Row {
            if (layout != ShellLayout.COMPACT && currentTab != null) {
                TaranginiNavRail(selected = currentTab, onSelect = navController::navigateToTopLevel)
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    Column {
                        val bottomBar = layout == ShellLayout.COMPACT && currentTab != null
                        if (showMiniPlayer) {
                            // With no bottom bar the mini player is the lowest thing on screen, and must stay
                            // above the gesture bar of the phone.
                            val aboveGestureBar = if (bottomBar) Modifier else Modifier.navigationBarsPadding()
                            miniPlayer({
                                navController.navigateToTopLevel(TopLevelDestination.PLAYER)
                            }, aboveGestureBar)
                        }
                        if (bottomBar) {
                            TaranginiBottomBar(selected = currentTab, onSelect = navController::navigateToTopLevel)
                        }
                    }
                },
                // Each screen has its own top bar, and that bar handles the status bar.
                contentWindowInsets = WindowInsets(),
            ) { innerPadding ->
                TaranginiNavHost(
                    navController = navController,
                    targets = targets,
                    modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
                    player = { player(it, Modifier) },
                    settings = settings,
                )
            }
            if (withPane) {
                player(targets, Modifier.width(NowPlayingPaneWidth).fillMaxHeight())
            }
        }
    }
}

// Wide enough for the cover and every button, and the tab screen keeps most of the window.
private val NowPlayingPaneWidth = 400.dp
