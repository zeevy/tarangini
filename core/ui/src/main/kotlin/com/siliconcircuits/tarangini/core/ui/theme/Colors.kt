/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// The app has a light look (Daylight) and a dark look (Night). Both use one green accent, and an
// orange for warnings and the heart. Cards and panels are a step lighter than the ground in both.
// Main buttons use the accent too, so `secondary` is the accent and the selected tab is a pale green.

private val Paper = Color(0xFFF4F1EA)
private val Ink = Color(0xFF15181D)
private val Green = Color(0xFF0D7050)
private val PaleGreen = Color(0xFFD5EBDF)
private val DeepGreen = Color(0xFF0A3D2B)
private val Orange = Color(0xFFB53B0B)

/** Daylight: warm paper, white cards, dark text and a deep green accent. */
internal val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = PaleGreen,
    onPrimaryContainer = DeepGreen,
    inversePrimary = Color(0xFF46D39A),
    secondary = Green,
    onSecondary = Color.White,
    secondaryContainer = PaleGreen,
    onSecondaryContainer = DeepGreen,
    tertiary = Orange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE3D6),
    onTertiaryContainer = Color(0xFF5A1C04),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECE8DF),
    onSurfaceVariant = Color(0xFF5B6270),
    inverseSurface = Color(0xFF2A2D31),
    inverseOnSurface = Paper,
    outline = Color(0xFF767C86),
    outlineVariant = Color(0xFFDDD8CC),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE6E2D9),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFBF8),
    surfaceContainer = Color.White,
    // Dialogs and menus. Light, so small and greyed text on them keeps its contrast.
    surfaceContainerHigh = Color(0xFFFAF8F3),
    surfaceContainerHighest = Color(0xFFECE8DF),
)

private val Night = Color(0xFF0F1311)
private val Mist = Color(0xFFEEF2EE)
private val Mint = Color(0xFF46D39A)
private val DarkMint = Color(0xFF1F3B2F)
private val LightMint = Color(0xFFBDEFD8)

/** Night: a dark green grey ground, cards a step lighter, light text and a bright green accent. */
internal val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF04261A),
    primaryContainer = DarkMint,
    onPrimaryContainer = LightMint,
    inversePrimary = Green,
    secondary = Mint,
    onSecondary = Color(0xFF04261A),
    secondaryContainer = DarkMint,
    onSecondaryContainer = LightMint,
    tertiary = Color(0xFFFF7A45),
    onTertiary = Color(0xFF2A0D02),
    tertiaryContainer = Color(0xFF4A2213),
    onTertiaryContainer = Color(0xFFFFD9C8),
    background = Night,
    onBackground = Mist,
    surface = Night,
    onSurface = Mist,
    surfaceVariant = Color(0xFF232A26),
    onSurfaceVariant = Color(0xFF9BA59F),
    inverseSurface = Mist,
    inverseOnSurface = Ink,
    outline = Color(0xFF75807A),
    outlineVariant = Color(0xFF2C3430),
    surfaceBright = Color(0xFF2E3531),
    surfaceDim = Night,
    surfaceContainerLowest = Color(0xFF0A0D0C),
    surfaceContainerLow = Color(0xFF131815),
    surfaceContainer = Color(0xFF1A1F1C),
    surfaceContainerHigh = Color(0xFF202622),
    surfaceContainerHighest = Color(0xFF262D29),
)

/**
 * The colours of a level meter: [normal] for most of the bar, [hot] near the top, [warn] at the top
 * and [off] for a segment that is not lit. Material 3 has no colours for this, so each look has its own.
 */
@Immutable
data class LevelColors(val normal: Color, val hot: Color, val warn: Color, val off: Color)

internal val LightLevelColors = LevelColors(Green, Color(0xFFB7791F), Color(0xFFD9480F), Color(0xFFE7E2D7))

internal val DarkLevelColors = LevelColors(Mint, Color(0xFFF2C14E), Color(0xFFFF7A45), Color(0xFF252C28))

/** The level meter colours of the look that is on, told by the ground colour of the theme. */
val levelColors: LevelColors
    @Composable @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.background.luminance() < HALF_LIGHT) DarkLevelColors else LightLevelColors

private const val HALF_LIGHT = 0.5f
