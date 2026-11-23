/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.domain.audio.LevelReading
import com.siliconcircuits.tarangini.core.domain.audio.SILENCE_DB
import com.siliconcircuits.tarangini.core.ui.R
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.LevelColors
import com.siliconcircuits.tarangini.core.ui.theme.levelColors
import kotlin.math.roundToInt

/**
 * A left and a right level meter of [SEGMENTS] segments each, lit from the bottom by [reading].
 * While [moving] is false, as in a pause, every segment is off. It only shows what is heard, so a
 * screen reader skips it. [reading] is called here and nowhere above, so a new level draws only
 * the meter again, not the screen around it.
 */
@Composable
fun LevelMeter(reading: () -> LevelReading, moving: Boolean, modifier: Modifier = Modifier) {
    val colors = levelColors
    val now = if (moving) reading() else LevelReading.SILENCE
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Side(litSegments(now.leftDb), stringResource(R.string.meter_left), colors)
        Side(litSegments(now.rightDb), stringResource(R.string.meter_right), colors)
    }
}

@Composable
private fun Side(lit: Int, label: String, colors: LevelColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // The top segment comes first in the column.
            for (segment in SEGMENTS - 1 downTo 0) {
                Box(
                    Modifier.size(width = 12.dp, height = 4.dp)
                        .background(segmentColor(segment, lit, colors), SegmentShape),
                )
            }
        }
        Text(
            text = label,
            fontFamily = DigitsFont,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun segmentColor(segment: Int, lit: Int, colors: LevelColors): Color = when {
    segment >= lit -> colors.off
    segment >= WARN_FROM -> colors.warn
    segment >= HOT_FROM -> colors.hot
    else -> colors.normal
}

/** How many segments [db] lights: none at silence, all at full scale, in even steps of dB between. */
internal fun litSegments(db: Float): Int =
    ((db - SILENCE_DB) / -SILENCE_DB * SEGMENTS).roundToInt().coerceIn(0, SEGMENTS)

private val SegmentShape = RoundedCornerShape(1.dp)

/** The segments of each side. */
const val SEGMENTS = 16

// The top five segments are the last 19 dB: amber, then orange for the last 8 dB.
private const val HOT_FROM = 11
private const val WARN_FROM = 14
