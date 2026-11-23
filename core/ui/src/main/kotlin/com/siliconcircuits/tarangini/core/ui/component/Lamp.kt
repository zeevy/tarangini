/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A small round lamp, as on a hardware player. A lit lamp has a soft light around it in [glow].
 * It only shows a state that the text or the label beside it also says, so it has no
 * description of its own.
 *
 * @param lit whether the lamp is on
 * @param on the colour of the lamp when it is on
 * @param off the colour of the lamp when it is off
 */
@Composable
fun Lamp(lit: Boolean, on: Color, off: Color, glow: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(LampSpace)) {
        val dot = LampSize.toPx() / 2
        if (lit) {
            drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), center, size.minDimension / 2))
        }
        drawCircle(if (lit) on else off, dot)
    }
}

private val LampSize = 6.dp

// The light around a lit lamp needs room of its own, so a lamp that lights up does not move the text.
private val LampSpace = 14.dp
