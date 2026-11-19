/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.siliconcircuits.tarangini.R
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.feature.home.navigation.HomeRoute
import com.siliconcircuits.tarangini.feature.library.navigation.FavouritesRoute
import com.siliconcircuits.tarangini.feature.library.navigation.LibraryRoute
import com.siliconcircuits.tarangini.feature.radio.navigation.RadioRoute

/** The tabs of the bottom bar, in the order they are shown. */
internal enum class TopLevelDestination(val route: Any, @StringRes val label: Int, @DrawableRes val icon: Int) {
    HOME(HomeRoute, R.string.tab_home, CoreUiR.drawable.ic_home),
    LIBRARY(LibraryRoute, R.string.tab_library, CoreUiR.drawable.ic_library),
    RADIO(RadioRoute, R.string.tab_radio, CoreUiR.drawable.ic_radio),
    FAVOURITES(FavouritesRoute, R.string.tab_favourites, CoreUiR.drawable.ic_favourite),
}
