/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.R

// Manrope is one file that holds every weight. Each weight the app uses is named here.
private val Manrope = FontFamily(
    listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(R.font.manrope, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

/**
 * JetBrains Mono, for the numbers of the now playing card: every digit is as wide as the next, so
 * a time that counts up does not move. The file holds only the Latin letters, digits and a few marks.
 */
val DigitsFont = FontFamily(
    listOf(FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(
            R.font.jetbrains_mono,
            weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

private fun TextStyle.manrope(weight: FontWeight? = null) =
    copy(fontFamily = Manrope, fontWeight = weight ?: fontWeight)

/** The Material 3 type scale in Manrope. Big titles are light, as large thin letters read well in both looks. */
internal val AppTypography = with(Typography()) {
    copy(
        displayLarge = displayLarge.manrope(FontWeight.Light),
        displayMedium = displayMedium.manrope(FontWeight.Light),
        displaySmall = displaySmall.manrope(FontWeight.Light),
        headlineLarge = headlineLarge.manrope(FontWeight.Light),
        headlineMedium = headlineMedium.manrope(FontWeight.Light),
        headlineSmall = headlineSmall.manrope(FontWeight.Light),
        titleLarge = titleLarge.manrope(),
        titleMedium = titleMedium.manrope(FontWeight.SemiBold),
        titleSmall = titleSmall.manrope(FontWeight.SemiBold),
        bodyLarge = bodyLarge.manrope(),
        bodyMedium = bodyMedium.manrope(),
        bodySmall = bodySmall.manrope(),
        labelLarge = labelLarge.manrope(FontWeight.SemiBold),
        labelMedium = labelMedium.manrope(FontWeight.SemiBold),
        labelSmall = labelSmall.manrope(FontWeight.SemiBold),
    )
}

/** Round corners everywhere: a cover has 14 dp, a panel 20 dp. */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
