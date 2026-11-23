/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The app theme. [darkTheme] picks the dark look ([DarkColors]) or the light look ([LightColors]);
 * by default it follows the dark mode of the phone. The text is the Material 3 type scale in
 * Manrope, and corners are round. The level meter colours ([levelColors]) and the
 * colours of the display and the lists ([lookColors]) follow the look.
 */
@Composable
fun TaranginiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLookColors provides if (darkTheme) DarkLook else LightLook) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
