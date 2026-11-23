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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// The app has a light look (Daylight) and a dark look (Night). Both use one green accent, and an
// orange for warnings and the heart. Screens lie on a ground colour; framed cards and the bars are
// a step lighter, and lists sit on their own plain card. Main buttons use the accent too, so
// `secondary` is the accent and the selected tab is a pale green.

private val Paper = Color(0xFFE9E6DF)
private val Frame = Color(0xFFF7F5F0)
private val Ink = Color(0xFF15181D)
private val Green = Color(0xFF0D7050)
private val PaleGreen = Color(0xFFD5EBDF)
private val DeepGreen = Color(0xFF0A3D2B)
private val Orange = Color(0xFFB53B0B)

/** Daylight: a warm grey ground, light framed cards, white lists, dark text and a deep green accent. */
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
    surfaceVariant = Color(0xFFE2DDD2),
    onSurfaceVariant = Color(0xFF545B68),
    inverseSurface = Color(0xFF20262B),
    inverseOnSurface = Paper,
    outline = Color(0xFF767C86),
    outlineVariant = Color(0xFFD6D1C5),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFDCD8CF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Frame,
    // Framed cards, the bottom bar and the rail.
    surfaceContainer = Frame,
    // Dialogs and menus. Light, so small and greyed text on them keeps its contrast.
    surfaceContainerHigh = Color(0xFFFAF8F3),
    surfaceContainerHighest = Color(0xFFE2DDD2),
)

private val Night = Color(0xFF0B0E0D)
private val NightFrame = Color(0xFF171C19)
private val Mist = Color(0xFFEEF2EE)
private val Mint = Color(0xFF46D39A)
private val DarkMint = Color(0xFF1F3B2F)
private val LightMint = Color(0xFFBDEFD8)

/** Night: a near black ground, framed cards a step lighter, light text and a bright green accent. */
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
    surfaceContainerLowest = Color(0xFF0F1311),
    surfaceContainerLow = Color(0xFF131815),
    surfaceContainer = NightFrame,
    surfaceContainerHigh = Color(0xFF202622),
    surfaceContainerHighest = Color(0xFF262D29),
)

/**
 * The colours of the new look that Material 3 has no place for: the display (a dark panel with
 * glowing text, dark in both looks), the plain list card, the lamps and the snackbar.
 *
 * @property frameEdge the thin edge of a framed card or list card
 * @property list the background of a list card; [listText] and [listMuted] are its texts
 * @property accentOnList the accent as text on a list card, such as the song that plays
 * @property rowOn the background of the row that plays
 * @property lcd the display background; [lcdText], [lcdMuted] and [lcdAccent] are its texts
 * @property lcdEdge thin lines and lamps that are off on the display
 * @property lcdTag the background of a lit tag or chip on the display
 * @property glow the light around lit text and lamps on the display
 * @property lampOff a lamp that is off on a framed card
 * @property hot and [warn] the top colours of the visual, and [warn] for an error line
 * @property heart a song marked as a favourite
 * @property danger an action that deletes, such as "Clear play history"
 * @property snack the snackbar, with [onSnack] text and [snackAction] for its button
 */
@Immutable
data class LookColors(
    val frameEdge: Color,
    val list: Color,
    val listText: Color,
    val listMuted: Color,
    val accentOnList: Color,
    val rowOn: Color,
    val lcd: Color,
    val lcdText: Color,
    val lcdMuted: Color,
    val lcdAccent: Color,
    val lcdEdge: Color,
    val lcdTag: Color,
    val glow: Color,
    val lampOff: Color,
    val hot: Color,
    val warn: Color,
    val heart: Color,
    val danger: Color,
    val snack: Color,
    val onSnack: Color,
    val snackAction: Color,
)

private val Hot = Color(0xFFF2C14E)
private val Warn = Color(0xFFFF7A45)

internal val LightLook = LookColors(
    frameEdge = Color(0x2414181E),
    list = Color.White,
    listText = Ink,
    listMuted = Color(0xFF5B6270),
    accentOnList = Green,
    rowOn = Color(0xFFE3EFE9),
    lcd = Color(0xFF122019),
    lcdText = Color(0xFFD7F3E6),
    lcdMuted = Color(0xFF8DB3A1),
    lcdAccent = Color(0xFF5FE0A8),
    lcdEdge = Color(0x14FFFFFF),
    lcdTag = Color(0x245FE0A8),
    glow = Color(0x595FE0A8),
    lampOff = Color(0xFFCFC9BC),
    hot = Hot,
    warn = Warn,
    heart = Color(0xFFC2410C),
    danger = Color(0xFFB42318),
    snack = Color(0xFF20262B),
    onSnack = Color(0xFFF3F5F4),
    snackAction = Color(0xFF5FE0A8),
)

internal val DarkLook = LookColors(
    frameEdge = Color(0x1AFFFFFF),
    list = Color(0xFF0F1311),
    listText = Mist,
    listMuted = Color(0xFF9BA59F),
    accentOnList = Mint,
    rowOn = Color(0xFF13261D),
    lcd = Color(0xFF060908),
    lcdText = Color(0xFFD7F3E6),
    lcdMuted = Color(0xFF7E9D8F),
    lcdAccent = Mint,
    lcdEdge = Color(0x12FFFFFF),
    lcdTag = Color(0x2446D39A),
    glow = Color(0x5946D39A),
    lampOff = Color(0xFF3A433E),
    hot = Hot,
    warn = Warn,
    heart = Warn,
    // The red of Daylight is too dark on the Night ground, so Night has a lighter one.
    danger = Color(0xFFFF8A7A),
    snack = Color(0xFFE7ECE9),
    onSnack = Color(0xFF101412),
    snackAction = Green,
)

internal val LocalLookColors = staticCompositionLocalOf { LightLook }

/** The colours of the new look for the look that is on. */
val lookColors: LookColors
    @Composable @ReadOnlyComposable
    get() = LocalLookColors.current
