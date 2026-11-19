/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
