/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.search.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.feature.search.SearchScreen

/** Opens the search screen. */
fun NavController.navigateToSearch() {
    navigate(SearchRoute) { launchSingleTop = true }
}

/**
 * The search screen. The screens of an album and an artist belong to the
 * library, so the app says how they open.
 */
fun NavGraphBuilder.searchScreen(onBack: () -> Unit, onOpenAlbum: (Album) -> Unit, onOpenArtist: (Artist) -> Unit) {
    composable<SearchRoute> { entry ->
        // A second tap that comes while the screen already changes is dropped.
        SearchScreen(
            onBack = { if (entry.isShown()) onBack() },
            onOpenAlbum = { if (entry.isShown()) onOpenAlbum(it) },
            onOpenArtist = { if (entry.isShown()) onOpenArtist(it) },
        )
    }
}

private fun NavBackStackEntry.isShown() = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
