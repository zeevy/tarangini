/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.siliconcircuits.tarangini.R
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.feature.library.navigation.FavouritesRoute
import com.siliconcircuits.tarangini.feature.library.navigation.LibraryRoute
import com.siliconcircuits.tarangini.feature.player.navigation.NowPlayingRoute

/** The tabs of the bottom bar, in the order they are shown. */
internal enum class TopLevelDestination(val route: Any, @StringRes val label: Int, @DrawableRes val icon: Int) {
    PLAYER(NowPlayingRoute, R.string.tab_player, CoreUiR.drawable.ic_player),
    LIBRARY(LibraryRoute, R.string.tab_library, CoreUiR.drawable.ic_library),
    FAVOURITES(FavouritesRoute, R.string.tab_favourites, CoreUiR.drawable.ic_favourite),
}
