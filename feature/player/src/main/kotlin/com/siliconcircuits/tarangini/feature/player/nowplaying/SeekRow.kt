/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The row under the display: Previous, the seek bar, the length of the song and Next. Holding
 * Previous or Next moves [SKIP_MS] back or on, again and again while it is held. A live stream has
 * no bar, only the keys.
 */
@Composable
internal fun SeekRow(state: NowPlayingUiState, actions: NowPlayingActions, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(RowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HoldKey(
            icon = CoreUiR.drawable.ic_skip_previous,
            label = stringResource(R.string.player_previous_hold),
            holdLabel = stringResource(R.string.player_rewind),
            onTap = actions.onPrevious,
            onHoldStep = { actions.onSeekBy(-SKIP_MS) },
        )
        val length = state.durationMs
        if (length == null) {
            Spacer(Modifier.weight(1f))
        } else {
            ThinSeekBar(state.positionMs, length, actions.onSeek, Modifier.weight(1f))
            Text(
                text = formatDuration(length).ifEmpty { ZERO },
                style = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HoldKey(
            icon = CoreUiR.drawable.ic_skip_next,
            label = stringResource(R.string.player_next_hold),
            holdLabel = stringResource(R.string.player_skip),
            onTap = actions.onNext,
            onHoldStep = { actions.onSeekBy(SKIP_MS) },
        )
    }
}

/**
 * A key of the seek row. A short tap runs [onTap]; a hold runs [onHoldStep] at once and then
 * every [REPEAT_MS] until the finger lifts. Each step gives a short vibration. TalkBack offers the
 * step as the long press and as an action of its own.
 */
@Composable
private fun HoldKey(icon: Int, label: String, holdLabel: String, onTap: () -> Unit, onHoldStep: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val tap by rememberUpdatedState(onTap)
    val step by rememberUpdatedState(onHoldStep)
    Box(
        modifier = Modifier
            .overhang(RowHeight)
            .size(width = KeyWidth, height = TouchHeight)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    var lifted = false
                    val up = withTimeoutOrNull(HOLD_MS) {
                        waitForUpOrCancellation().also { lifted = true }
                    }
                    if (lifted) {
                        if (up != null) tap()
                    } else {
                        do {
                            step()
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            var ended = false
                            withTimeoutOrNull(REPEAT_MS) {
                                waitForUpOrCancellation()
                                ended = true
                            }
                        } while (!ended)
                    }
                }
            }
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick {
                    tap()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(holdLabel) {
                        step()
                        true
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, Modifier.size(22.dp))
    }
}

/**
 * A thin bar with a small grip. While the grip is dragged the bar shows the place of the finger,
 * and the player is told only when the finger lifts: a seek for every step of a drag would make
 * the sound stutter. The touch area is 48 dp high, more than the row it sits in.
 */
// A slider with its own grip and bar is API of Material 3 that may still change.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThinSeekBar(positionMs: Long, lengthMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var dragged by remember { mutableStateOf<Float?>(null) }
    val shown = dragged ?: positionMs.toFloat()
    val played = formatDuration(shown.toLong()).ifEmpty { ZERO }
    val total = formatDuration(lengthMs).ifEmpty { ZERO }
    val label = stringResource(R.string.now_playing_position)
    // A screen reader says the times, not a bare percentage.
    val place = stringResource(R.string.now_playing_position_state, played, total)
    val slider = remember(lengthMs) { SliderState(trackRange = 0f..lengthMs.toFloat()) }
    // The slider shows what this screen says, so a new place from the player moves the bar too.
    slider.value = shown.coerceIn(0f, lengthMs.toFloat())
    val part = if (lengthMs > 0) (slider.value / lengthMs).coerceIn(0f, 1f) else 0f
    val accent = MaterialTheme.colorScheme.primary
    val groove = MaterialTheme.colorScheme.outlineVariant
    val look = lookColors
    Slider(
        state = slider,
        onValueChange = { dragged = it },
        onValueChangeFinished = {
            dragged?.let { onSeek(it.toLong()) }
            dragged = null
        },
        modifier = modifier.overhang(RowHeight).semantics {
            contentDescription = label
            stateDescription = place
        },
        thumb = {
            Box(
                Modifier
                    .size(width = GripWidth, height = GripHeight)
                    .background(look.list, MaterialTheme.shapes.extraSmall)
                    .border(1.dp, look.frameEdge, MaterialTheme.shapes.extraSmall),
            )
        },
        track = {
            Box(
                Modifier.fillMaxWidth().height(BarHeight).drawBehind {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(groove, cornerRadius = radius)
                    drawRoundRect(accent, Offset.Zero, Size(size.width * part, size.height), radius)
                },
            )
        },
    )
}

/**
 * Lays the content out at its own height but takes only [visible] of height in the row, the rest
 * standing over the edges. The keys keep their 48 dp of touch in a row of 32 dp.
 */
internal fun Modifier.overhang(visible: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Int.MAX_VALUE))
    val height = visible.roundToPx()
    layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
}

/** How far a hold of Previous or Next moves in the song each time. */
internal const val SKIP_MS = 10_000L

private const val HOLD_MS = 450L
private const val REPEAT_MS = 400L
private val RowHeight = 32.dp
private val TouchHeight = 48.dp
private val KeyWidth = 44.dp
private val BarHeight = 3.dp
private val GripWidth = 10.dp
private val GripHeight = 11.dp
