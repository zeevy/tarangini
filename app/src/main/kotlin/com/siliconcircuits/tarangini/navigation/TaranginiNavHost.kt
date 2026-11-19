/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.siliconcircuits.tarangini.feature.home.navigation.HomeRoute
import com.siliconcircuits.tarangini.feature.home.navigation.homeScreen
import com.siliconcircuits.tarangini.feature.library.navigation.favouritesScreen
import com.siliconcircuits.tarangini.feature.library.navigation.libraryScreen
import com.siliconcircuits.tarangini.feature.radio.navigation.radioScreen
import com.siliconcircuits.tarangini.feature.settings.navigation.navigateToSettings
import com.siliconcircuits.tarangini.feature.settings.navigation.settingsScreen

/** The one navigation graph of the app. Features meet only here. */
@Composable
internal fun TaranginiNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        homeScreen(onSettingsClick = navController::navigateToSettings)
        libraryScreen()
        radioScreen()
        favouritesScreen()
        settingsScreen(onBackClick = navController::popBackStack)
    }
}
