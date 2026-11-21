/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.feature.home.navigation.HomeRoute
import com.siliconcircuits.tarangini.feature.home.navigation.homeScreen
import com.siliconcircuits.tarangini.feature.library.navigation.favouritesScreen
import com.siliconcircuits.tarangini.feature.library.navigation.libraryGraph
import com.siliconcircuits.tarangini.feature.library.navigation.navigateToAlbum
import com.siliconcircuits.tarangini.feature.library.navigation.navigateToArtist
import com.siliconcircuits.tarangini.feature.library.navigation.navigateToPlaylist
import com.siliconcircuits.tarangini.feature.player.navigation.playerScreens
import com.siliconcircuits.tarangini.feature.search.navigation.navigateToSearch
import com.siliconcircuits.tarangini.feature.search.navigation.searchScreen
import com.siliconcircuits.tarangini.feature.settings.navigation.licencesScreen
import com.siliconcircuits.tarangini.feature.settings.navigation.navigateToLicences
import com.siliconcircuits.tarangini.feature.settings.navigation.navigateToSettings
import com.siliconcircuits.tarangini.feature.settings.navigation.settingsScreen

/** The one navigation graph of the app. Features meet only here. */
@Composable
internal fun TaranginiNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    home: @Composable (HomeTargets) -> Unit = {},
    settings: @Composable (onOpenLicences: () -> Unit) -> Unit = {},
) {
    val targets = remember(navController) {
        // A second tap that comes while the screen already changes is dropped.
        val onHome = { navController.currentDestination?.hasRoute<HomeRoute>() == true }
        HomeTargets(
            onOpenLibrary = { if (onHome()) navController.navigateToTopLevel(TopLevelDestination.LIBRARY) },
            // The playlist opens on top of the Library tab, so Back and the tab lead to the lists.
            onOpenPlaylist = { playlist ->
                if (onHome()) {
                    navController.navigateToTopLevel(TopLevelDestination.LIBRARY)
                    navController.navigateToPlaylist(playlist)
                }
            },
        )
    }
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        homeScreen(onSettingsClick = navController::navigateToSettings) { home(targets) }
        libraryGraph(navController, onSearch = navController::navigateToSearch)
        searchScreen(
            onBack = navController::popBackStack,
            onOpenAlbum = navController::navigateToAlbum,
            onOpenArtist = navController::navigateToArtist,
        )
        favouritesScreen()
        settingsScreen(onBackClick = navController::popBackStack) { settings(navController::navigateToLicences) }
        licencesScreen(onBack = navController::popBackStack)
        playerScreens(navController)
    }
}

/** Where the lists of Home lead to. */
internal class HomeTargets(val onOpenLibrary: () -> Unit, val onOpenPlaylist: (Playlist) -> Unit)
