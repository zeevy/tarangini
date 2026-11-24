/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.siliconcircuits.tarangini.feature.library.navigation.favouritesScreen
import com.siliconcircuits.tarangini.feature.library.navigation.libraryGraph
import com.siliconcircuits.tarangini.feature.library.navigation.navigateToAlbum
import com.siliconcircuits.tarangini.feature.library.navigation.navigateToArtist
import com.siliconcircuits.tarangini.feature.player.navigation.NowPlayingRoute
import com.siliconcircuits.tarangini.feature.player.navigation.playerScreen
import com.siliconcircuits.tarangini.feature.player.nowplaying.PlayerTargets
import com.siliconcircuits.tarangini.feature.search.navigation.navigateToSearch
import com.siliconcircuits.tarangini.feature.search.navigation.searchScreen
import com.siliconcircuits.tarangini.feature.settings.navigation.licencesScreen
import com.siliconcircuits.tarangini.feature.settings.navigation.navigateToLicences
import com.siliconcircuits.tarangini.feature.settings.navigation.navigateToSettings
import com.siliconcircuits.tarangini.feature.settings.navigation.settingsScreen

/** The one navigation graph of the app. Features meet only here. The player is the first tab. */
@Composable
internal fun TaranginiNavHost(
    navController: NavHostController,
    targets: PlayerTargets,
    modifier: Modifier = Modifier,
    player: @Composable (PlayerTargets) -> Unit = {},
    settings: @Composable (onOpenLicences: () -> Unit) -> Unit = {},
) {
    NavHost(navController = navController, startDestination = NowPlayingRoute, modifier = modifier) {
        playerScreen(targets, player)
        libraryGraph(
            navController,
            onSearch = navController::navigateToSearch,
            onSettings = navController::navigateToSettings,
        )
        searchScreen(
            onBack = navController::popBackStack,
            onOpenAlbum = navController::navigateToAlbum,
            onOpenArtist = navController::navigateToArtist,
        )
        favouritesScreen(onSearch = navController::navigateToSearch)
        settingsScreen(onBackClick = navController::popBackStack) { settings(navController::navigateToLicences) }
        licencesScreen(onBack = navController::popBackStack)
    }
}

/**
 * Where the player leads to. The album opens on top of the Library tab, so Back and the tab lead
 * to the lists of the library.
 */
internal fun playerTargets(navController: NavHostController) = PlayerTargets(
    onOpenSearch = navController::navigateToSearch,
    onOpenSettings = navController::navigateToSettings,
    onOpenLibrary = { navController.navigateToTopLevel(TopLevelDestination.LIBRARY) },
    onOpenAlbum = { id, title, artist ->
        navController.navigateToTopLevel(TopLevelDestination.LIBRARY)
        navController.navigateToAlbum(id, title, artist)
    },
)
