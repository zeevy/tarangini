/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The app theme. There is one look, and it is dark: the colours are
 * [GlassColors], the text is the Material 3 type scale in Manrope, and
 * corners are round.
 */
@Composable
fun TaranginiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlassColors,
        typography = GlassTypography,
        shapes = GlassShapes,
        content = content,
    )
}
