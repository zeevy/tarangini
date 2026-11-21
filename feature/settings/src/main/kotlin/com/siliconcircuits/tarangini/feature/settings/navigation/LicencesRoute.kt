/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.settings.licences.LicencesScreen
import kotlinx.serialization.Serializable

/** The open source licences screen. It opens from Settings. */
@Serializable
data object LicencesRoute

fun NavController.navigateToLicences() {
    navigate(LicencesRoute) { launchSingleTop = true }
}

fun NavGraphBuilder.licencesScreen(onBack: () -> Unit) {
    composable<LicencesRoute> { LicencesScreen(onBack = onBack) }
}
