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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.siliconcircuits.tarangini.feature.player.navigation.isPlayerScreen
import com.siliconcircuits.tarangini.feature.player.navigation.navigateToNowPlaying

/**
 * The frame every screen sits in: the screens, then the [miniPlayer] slot,
 * then the bottom bar. The bottom bar shows only on the tabs. Each screen
 * draws its own top bar. The slot is part of the bottom bar area, so a list
 * ends above it and no row is covered.
 *
 * On a wide window the tabs move to a rail at the left, and on a tablet the
 * [nowPlayingPane] stands at the right of the tab screens in place of the mini
 * player. Folding or turning the device keeps the screen, its scroll place and
 * the playback: only the layout around the screens changes.
 *
 * @param miniPlayer the bar with the current item. It gets the way to open the now playing screen,
 *   and the modifier it must put on itself
 * @param nowPlayingPane the now playing screen as a pane, with the controller that opens the
 *   queue and the equalizer, and the modifier it must put on itself
 */
@Composable
internal fun TaranginiShell(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    miniPlayer: @Composable (onOpen: () -> Unit, modifier: Modifier) -> Unit = { _, _ -> },
    nowPlayingPane: @Composable (NavHostController, Modifier) -> Unit = { _, _ -> },
    home: @Composable (HomeTargets) -> Unit = {},
    settings: @Composable (onOpenLicences: () -> Unit) -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = destination?.topLevelDestination()

    BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        val layout = ShellLayout.of(maxWidth, maxHeight)
        val withPane = layout == ShellLayout.EXPANDED && currentTab != null
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
                            miniPlayer(navController::navigateToNowPlaying, aboveGestureBar)
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
                    modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
                    home = home,
                    settings = settings,
                )
            }
            if (withPane) {
                nowPlayingPane(navController, Modifier.width(NowPlayingPaneWidth).fillMaxHeight())
            }
        }
    }
}

// Wide enough for the cover and every button, and the tab screen keeps most of the window.
private val NowPlayingPaneWidth = 400.dp
