/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// The one look of the app: a dark screen with glass panels, white main buttons and one cyan accent.
// It is dark only, so it reads the same on the phone and on the screen of a car at night.

private val Ink = Color(0xFF06080F)
private val Paper = Color(0xFFF2F5FA)
private val Cyan = Color(0xFF5CE1FF)

internal val GlassColors = darkColorScheme(
    // The accent: the playing row, the progress line, a link.
    primary = Cyan,
    onPrimary = Ink,
    primaryContainer = Color(0xFF0F1E27),
    onPrimaryContainer = Paper,
    inversePrimary = Color(0xFF00677F),
    // Main buttons and the selected tab are white with dark content.
    secondary = Paper,
    onSecondary = Ink,
    secondaryContainer = Paper,
    onSecondaryContainer = Ink,
    tertiary = Color(0xFFB79BFF),
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF1E1638),
    onTertiaryContainer = Paper,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = Color(0xFF1A1F30),
    onSurfaceVariant = Color(0xFFA7B0C3),
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    outline = Color(0xFF8A95AD),
    // The thin edge of a panel: white at 12 percent on the background.
    outlineVariant = Color(0xFF24262C),
    surfaceBright = Color(0xFF2A3148),
    surfaceDim = Ink,
    // The panels, from the background up to the lightest one.
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Color(0xFF0E111B),
    surfaceContainer = Color(0xFF141826),
    surfaceContainerHigh = Color(0xFF1A1F30),
    surfaceContainerHighest = Color(0xFF20263A),
)
