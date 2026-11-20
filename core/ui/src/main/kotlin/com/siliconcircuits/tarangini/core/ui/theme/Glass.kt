/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PurpleGlow = Color(0xFF7A3CFF)
private val BlueGlow = Color(0xFF2B5BFF)
private const val PURPLE_STRENGTH = 0.30f
private const val BLUE_STRENGTH = 0.22f
private const val BLUE_GLOW_HEIGHT = 0.6f

/**
 * The background of every screen: the dark base with a soft purple light
 * from the top left corner and a blue one from the right edge. Put it on
 * the frame of a screen, and give that frame no colour of its own.
 */
fun Modifier.glassBackground(): Modifier = drawBehind {
    drawRect(GlassColors.background)
    val reach = size.width
    if (reach > 0f) {
        drawRect(
            Brush.radialGradient(
                listOf(PurpleGlow.copy(alpha = PURPLE_STRENGTH), Color.Transparent),
                center = Offset.Zero,
                radius = reach,
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(BlueGlow.copy(alpha = BLUE_STRENGTH), Color.Transparent),
                center = Offset(size.width, size.height * BLUE_GLOW_HEIGHT),
                radius = reach,
            ),
        )
    }
}

/** A top bar with no colour of its own, so the background of the screen shows through it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun glassTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
)

/** The colour of a glass panel that floats over a screen, such as the bottom bar. */
val GlassPanel = Color(0xD9161B2A)

/** The thin light edge of a glass panel. */
val GlassEdge = BorderStroke(1.dp, Color(0x1FFFFFFF))
