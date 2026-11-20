/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.radio.RadioScreen
import com.siliconcircuits.tarangini.feature.radio.form.StationFormScreen

/** Adds the Radio tab and the form that adds or changes a station. */
fun NavGraphBuilder.radioScreens(navController: NavController) {
    composable<RadioRoute> {
        RadioScreen(
            onAdd = { navController.openForm(StationFormRoute()) },
            onEdit = { station -> navController.openForm(StationFormRoute(station.id)) },
        )
    }
    composable<StationFormRoute> {
        // A second Back while the form closes must not close the tab under it as well.
        StationFormScreen(onBack = { if (navController.isOnForm()) navController.popBackStack() })
    }
}

private fun NavController.isOnForm(): Boolean = currentDestination?.hasRoute<StationFormRoute>() == true

/** Two quick taps open one form, not two. */
private fun NavController.openForm(route: StationFormRoute) {
    if (!isOnForm()) navigate(route)
}
