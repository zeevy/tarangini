/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.feature.library.LibraryScreen
import com.siliconcircuits.tarangini.feature.library.album.AlbumScreen
import com.siliconcircuits.tarangini.feature.library.artist.ArtistScreen
import com.siliconcircuits.tarangini.feature.library.favourites.FavouritesScreen
import com.siliconcircuits.tarangini.feature.library.folder.FolderScreen

/**
 * The screens of the Library tab. The detail screens are inside the group
 * of [LibraryRoute], so the tab stays marked while one of them is open and
 * comes back to it when the person returns to the tab.
 */
fun NavGraphBuilder.libraryGraph(navController: NavController) {
    navigation<LibraryRoute>(startDestination = LibraryListsRoute) {
        composable<LibraryListsRoute> { entry ->
            LibraryScreen(
                onOpenAlbum = { navController.open(entry, it.route()) },
                onOpenArtist = { navController.open(entry, it.route()) },
                onOpenFolder = { navController.open(entry, it.route()) },
            )
        }
        composable<AlbumRoute> { entry -> AlbumScreen(onBack = { navController.back(entry) }) }
        composable<ArtistRoute> { entry ->
            ArtistScreen(
                onBack = { navController.back(entry) },
                onOpenAlbum = { navController.open(entry, it.route()) },
            )
        }
        composable<FolderRoute> { entry ->
            FolderScreen(
                onBack = { navController.back(entry) },
                onOpenFolder = { navController.open(entry, it.route()) },
            )
        }
    }
}

fun NavGraphBuilder.favouritesScreen() {
    composable<FavouritesRoute> { FavouritesScreen() }
}

// A second tap that comes while the screen already changes is dropped. Without this, two fast
// taps on a row open the screen twice, and two fast taps on Back leave the tab.
private fun NavController.open(from: NavBackStackEntry, route: Any) {
    if (from.isShown()) navigate(route)
}

private fun NavController.back(from: NavBackStackEntry) {
    if (from.isShown()) popBackStack()
}

private fun NavBackStackEntry.isShown() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

// A text from a file tag can be very long. The top bar shows one line, and the saved state of the
// screens has a size limit, so the route carries only the start of it.
private fun Album.route() = AlbumRoute(id, title.take(MAX_TITLE), artist.take(MAX_TITLE))

private fun Artist.route() = ArtistRoute(id, name.take(MAX_TITLE))

private fun Folder.route() = FolderRoute(id, name.take(MAX_TITLE))

private const val MAX_TITLE = 200
