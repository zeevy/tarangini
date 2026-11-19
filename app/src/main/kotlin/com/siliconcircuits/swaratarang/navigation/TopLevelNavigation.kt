/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * Switches to the tab [destination]. The tab that is left keeps its screens
 * and their state, and gets them back when the user returns to it. Only one
 * tab is on the back stack at a time, so Back from the first screen of any
 * tab leaves the app.
 */
internal fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** The tab this destination belongs to, or null when it is not inside a tab. */
internal fun NavDestination.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { tab -> hierarchy.any { it.hasRoute(tab.route::class) } }
