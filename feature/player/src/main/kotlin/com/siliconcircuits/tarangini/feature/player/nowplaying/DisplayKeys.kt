/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.ChipKind
import com.siliconcircuits.tarangini.core.ui.component.ChipPlace
import com.siliconcircuits.tarangini.core.ui.component.LedChip
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.player.R

/** The play mark, the time and the two mode chips. */
@Composable
internal fun TopLine(state: NowPlayingUiState?, actions: NowPlayingActions, large: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state != null) PlayMark(state.showPause, actions.onTogglePlay)
        TimeButton(state, large)
        Spacer(Modifier.weight(1f))
        // A live stream has no order to change and no end to repeat.
        if (state != null && !state.isLive) {
            LedChip(
                text = null,
                lit = state.shuffle,
                onClick = actions.onToggleShuffle,
                place = ChipPlace.DISPLAY,
                kind = ChipKind.SWITCH,
                icon = painterResource(CoreUiR.drawable.ic_shuffle),
                label = stringResource(R.string.now_playing_shuffle),
                height = DisplayChipHeight,
            )
            RepeatChip(state.repeat, actions.onCycleRepeat)
        }
    }
}

@Composable
private fun RepeatChip(repeat: RepeatMode, onCycle: () -> Unit) {
    LedChip(
        text = null,
        lit = repeat != RepeatMode.OFF,
        onClick = onCycle,
        place = ChipPlace.DISPLAY,
        kind = ChipKind.BUTTON,
        icon = painterResource(
            if (repeat ==
                RepeatMode.ONE
            ) {
                CoreUiR.drawable.ic_repeat_one
            } else {
                CoreUiR.drawable.ic_repeat
            },
        ),
        label = stringResource(
            when (repeat) {
                RepeatMode.OFF -> R.string.now_playing_repeat_off
                RepeatMode.ALL -> R.string.now_playing_repeat_all
                RepeatMode.ONE -> R.string.now_playing_repeat_one
            },
        ),
        height = DisplayChipHeight,
    )
}

/** The large play or pause mark. Its name for a screen reader is what a tap does. */
@Composable
private fun PlayMark(showPause: Boolean, onToggle: () -> Unit) {
    val look = lookColors
    val label = stringResource(if (showPause) CoreUiR.string.action_pause else CoreUiR.string.action_play)
    Box(
        modifier = Modifier
            .pullStart(PlayMarkPull)
            .size(width = 44.dp, height = 48.dp)
            .clickable(role = Role.Button, onClickLabel = null, onClick = onToggle)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        // A soft light behind the mark, as on a lit display.
        Box(Modifier.size(26.dp).background(look.glow, CircleShape))
        Icon(
            painter = painterResource(if (showPause) CoreUiR.drawable.ic_pause else CoreUiR.drawable.ic_play),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = look.lcdAccent,
        )
    }
}

/** The time in large digits. A tap switches between the time played and the time left. */
@Composable
private fun TimeButton(state: NowPlayingUiState?, large: Boolean) {
    val look = lookColors
    var showsLeft by rememberSaveable { mutableStateOf(false) }
    val length = state?.durationMs
    val canSwitch = state != null && length != null
    val left = canSwitch && showsLeft
    val time = shownTime(state, left)
    val spoken = spokenTime(state, time, canSwitch, left)
    val switchLabel = stringResource(if (left) R.string.player_time_show_played else R.string.player_time_show_left)
    val switch = { showsLeft = !showsLeft }
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .then(
                if (canSwitch) {
                    Modifier.clickable(
                        onClickLabel = switchLabel,
                        role = Role.Button,
                        onClick = switch,
                    )
                } else {
                    Modifier
                },
            )
            // The click above keeps its role and label; only the words are set here.
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = time,
            style = TextStyle(
                fontFamily = DigitsFont,
                fontWeight = FontWeight.Bold,
                fontSize = if (large) 52.sp else 34.sp,
                letterSpacing = 0.6.sp,
                shadow = Shadow(look.glow, blurRadius = 20f),
            ),
            color = if (state == null) look.lcdMuted else look.lcdAccent,
            maxLines = 1,
        )
        if (left) Text(text = stringResource(R.string.player_time_left_word), style = SmallWord, color = look.lcdMuted)
    }
}

/** The time as the display shows it: played, left, or a word for a live item. */
@Composable
private fun shownTime(state: NowPlayingUiState?, left: Boolean): String {
    val length = state?.durationMs
    return when {
        state == null -> ZERO
        length == null -> stringResource(if (state.isBuffering) R.string.player_loading else R.string.now_playing_live)
        left -> formatDuration((length - state.positionMs).coerceAtLeast(0)).ifEmpty { ZERO }
        else -> formatDuration(state.positionMs).ifEmpty { ZERO }
    }
}

/** What TalkBack says for the time: "1:05 played", "2:00 left", or the word itself. */
@Composable
private fun spokenTime(state: NowPlayingUiState?, time: String, canSwitch: Boolean, left: Boolean): String = when {
    !canSwitch -> if (state == null) stringResource(R.string.player_time_none) else time
    left -> stringResource(R.string.player_time_left, time)
    else -> stringResource(R.string.player_time_played, time)
}

private fun Modifier.pullStart(by: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val pull = by.roundToPx()
    layout(placeable.width - pull, placeable.height) { placeable.place(-pull, 0) }
}

private val PlayMarkPull = 8.dp
private val DisplayChipHeight = 30.dp
