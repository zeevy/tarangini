/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.library.LibraryScreen
import com.siliconcircuits.tarangini.feature.library.favourites.FavouritesScreen

fun NavGraphBuilder.libraryScreen() {
    composable<LibraryRoute> { LibraryScreen() }
}

fun NavGraphBuilder.favouritesScreen() {
    composable<FavouritesRoute> { FavouritesScreen() }
}
