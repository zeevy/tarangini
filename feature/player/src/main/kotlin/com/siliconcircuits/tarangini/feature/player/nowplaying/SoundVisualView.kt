/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.LookColors
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The visual of the display: bars of the spectrum with a cap over each, a wave, or a dashed line
 * when it is off. A tap moves to the next mode. [shown] is read only while drawing, so the 25
 * pictures a second redraw the visual alone and not the screen around it.
 */
@Composable
internal fun SoundVisualView(
    mode: VisualMode,
    shown: () -> ShownVisual,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = VisualHeight,
) {
    val look = lookColors
    val name = stringResource(
        when (mode) {
            VisualMode.BARS -> R.string.player_visual_bars
            VisualMode.WAVE -> R.string.player_visual_wave
            VisualMode.OFF -> R.string.player_visual_off
        },
    )
    val label = stringResource(R.string.player_visual, name)
    val change = stringResource(R.string.player_visual_change)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClickLabel = change, onClick = onCycle)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick(change) {
                    onCycle()
                    true
                }
            },
    ) {
        when (mode) {
            VisualMode.BARS -> Canvas(Modifier.fillMaxSize()) { drawBars(shown(), look) }
            VisualMode.WAVE -> Canvas(Modifier.fillMaxSize()) { drawWave(shown().wave, look) }
            VisualMode.OFF -> OffLine(look)
        }
        if (mode != VisualMode.OFF) {
            Text(
                text = name.uppercase(),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp),
                style = SmallWord,
                color = look.lcdMuted,
            )
        }
    }
}

@Composable
private fun OffLine(look: LookColors) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2
            drawLine(
                look.lcdEdge,
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )
        }
        Text(
            text = stringResource(R.string.player_visual_off_text),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = SmallWord,
            color = look.lcdMuted,
        )
    }
}

// Each bar is lit from the bottom: the accent, then hot, then warn near the top, as on a hardware meter.
private fun DrawScope.drawBars(shown: ShownVisual, look: LookColors) {
    val count = shown.bands.size
    if (count == 0) return
    val gap = BarGap.toPx()
    val width = (size.width - gap * (count - 1)) / count
    val fill = Brush.verticalGradient(
        0f to look.warn,
        HOT_TOP to look.warn,
        HOT_TOP to look.hot,
        ACCENT_TOP to look.hot,
        ACCENT_TOP to look.lcdAccent,
        1f to look.lcdAccent,
        startY = 0f,
        endY = size.height,
    )
    val cap = PeakHeight.toPx()
    shown.bands.forEachIndexed { index, band ->
        val x = index * (width + gap)
        // A bar never falls below a thin line, so a quiet band still shows where it is.
        val barHeight = size.height * band.coerceIn(MIN_BAR, 1f)
        drawRect(fill, Offset(x, size.height - barHeight), Size(width, barHeight))
        val peak = shown.peaks.getOrElse(index) { 0f }
        if (peak > MIN_BAR) {
            val top = (size.height * (1f - peak)).coerceIn(0f, size.height - cap)
            drawRect(look.lcdText, Offset(x, top), Size(width, cap))
        }
    }
    // Thin dark lines across the bars make the segments of a hardware display.
    val line = 1.dp.toPx()
    val step = 4.dp.toPx()
    var y = size.height - step
    while (y > 0f) {
        drawRect(look.lcd, Offset(0f, y), Size(size.width, line))
        y -= step
    }
}

private fun DrawScope.drawWave(wave: List<Float>, look: LookColors) {
    if (wave.size < 2) return
    val middle = size.height / 2
    val stepX = size.width / (wave.size - 1)
    val path = Path()
    wave.forEachIndexed { index, sample ->
        val point = Offset(index * stepX, middle - sample * middle * WAVE_FILL)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(path, look.glow, style = Stroke(width = 5.dp.toPx()))
    drawPath(path, look.lcdAccent, style = Stroke(width = 2.dp.toPx()))
}

/** The height of the visual on a phone. */
internal val VisualHeight = 52.dp

/** The height of the visual on a tall screen, such as a tablet held upright. */
internal val LargeVisualHeight = 104.dp
private val BarGap = 2.dp
private val PeakHeight = 2.dp
private const val MIN_BAR = 0.06f
private const val ACCENT_TOP = 0.45f
private const val HOT_TOP = 0.18f

// The wave keeps a little room at the top and the bottom of the display.
private const val WAVE_FILL = 0.9f

internal val SmallWord = TextStyle(
    fontFamily = DigitsFont,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 9.sp,
    letterSpacing = 0.1.sp,
)
