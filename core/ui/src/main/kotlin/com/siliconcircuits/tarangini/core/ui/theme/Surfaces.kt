/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Colours for a top bar that lies on the screen background: the bar has no colour of its own, so
 * the ground shows through, also when the list under it scrolls.
 */
@OptIn(ExperimentalMaterial3Api::class) // The top bar colours are still marked experimental in Material 3.
@Composable
fun clearTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
)

/** The colour of a card or a floating panel, such as the mini player and the bottom bar. */
val PanelColor: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceContainer

/** The thin edge around a card or a floating panel, so it stands out from the ground in both looks. */
val PanelEdge: BorderStroke
    @Composable
    @ReadOnlyComposable
    get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
