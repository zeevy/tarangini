/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * The frame every screen sits in: the screens, then the [miniPlayer] slot,
 * then the bottom bar. The bottom bar shows only on the tabs. Each screen
 * draws its own top bar.
 */
@Composable
fun SwaraTarangShell(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    miniPlayer: @Composable () -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.topLevelDestination()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                miniPlayer()
                if (currentTab != null) {
                    SwaraTarangBottomBar(selected = currentTab, onSelect = navController::navigateToTopLevel)
                }
            }
        },
        // Each screen has its own top bar, and that bar handles the status bar.
        contentWindowInsets = WindowInsets(),
    ) { innerPadding ->
        SwaraTarangNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        )
    }
}
