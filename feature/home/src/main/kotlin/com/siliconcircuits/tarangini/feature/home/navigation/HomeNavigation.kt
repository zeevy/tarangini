/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.siliconcircuits.tarangini.feature.home.HomeScreen

fun NavGraphBuilder.homeScreen(onSettingsClick: () -> Unit) {
    composable<HomeRoute> { HomeScreen(onSettingsClick = onSettingsClick) }
}
