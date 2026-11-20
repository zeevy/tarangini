/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.settings.SettingsScreen

fun NavController.navigateToSettings() {
    navigate(SettingsRoute) { launchSingleTop = true }
}

fun NavGraphBuilder.settingsScreen(onBackClick: () -> Unit, content: @Composable () -> Unit) {
    composable<SettingsRoute> { SettingsScreen(onBackClick = onBackClick, content = content) }
}
