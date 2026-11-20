/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The seek bar with the time played and the length under it.
 *
 * While the thumb is dragged the bar shows the place of the finger, and the player is told only
 * when the finger lifts. A seek for every step of a drag would make the sound stutter.
 */
// A slider with its own dot and bar is API of Material 3 that may still change.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SeekBar(state: NowPlayingUiState, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val length = state.durationMs ?: return
    var dragged by remember { mutableStateOf<Float?>(null) }
    val shown = dragged ?: state.positionMs.toFloat()
    val played = formatDuration(shown.toLong()).ifEmpty { ZERO }
    val total = formatDuration(length).ifEmpty { ZERO }
    val label = stringResource(R.string.now_playing_position)
    // A screen reader says the times, not a bare percentage.
    val place = stringResource(R.string.now_playing_position_state, played, total)
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = shown.coerceIn(0f, length.toFloat()),
            onValueChange = { dragged = it },
            onValueChangeFinished = {
                dragged?.let { onSeek(it.toLong()) }
                dragged = null
            },
            valueRange = 0f..length.toFloat(),
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = place
            },
            // No handle: the end of the filled part is the place. The whole bar takes the touch.
            thumb = { Spacer(modifier = Modifier.size(width = NoGap, height = BarHeight)) },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(BarHeight),
                    // The part not played yet is a dim panel colour, so the played part stands out.
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    drawStopIndicator = null,
                    thumbTrackGapSize = NoGap,
                )
            },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Time(played)
            Time(total)
        }
    }
}

@Composable
private fun Time(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// The height of the bar. The area that takes a touch stays the 48 dp of the slider.
private val BarHeight = 12.dp
private val NoGap = 0.dp

// The shared format leaves a zero out, which is right for a length that is not known. A place is never unknown.
private const val ZERO = "0:00"
