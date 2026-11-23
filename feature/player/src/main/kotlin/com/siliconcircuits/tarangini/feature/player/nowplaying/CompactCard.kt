/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.FrameCard
import com.siliconcircuits.tarangini.core.ui.component.LcdPanel
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The player in one line, shown in place of the whole card while Up next scrolls: a few bars, the
 * song, the time, Play and Next, and a thin line of the place in the song. A tap on the song shows
 * the whole card again.
 */
@Composable
internal fun CompactCard(
    state: NowPlayingUiState,
    actions: NowPlayingActions,
    shown: () -> ShownVisual,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = lookColors
    val title = state.title ?: stringResource(CoreUiR.string.unknown_title)
    val artist = state.artist ?: stringResource(CoreUiR.string.unknown_artist)
    val time = shownTime(state, left = false)
    val part = state.durationMs?.takeIf { it > 0 }?.let { (state.positionMs.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
    val moving = state.showPause && !state.isBuffering && state.error == null
    FrameCard(modifier = modifier, padding = PaddingValues(6.dp)) {
        LcdPanel(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val line = 2.dp.toPx()
                    drawRect(look.lcdAccent, Offset(0f, size.height - line), Size(size.width * part, line))
                },
            padding = PaddingValues(0.dp),
        ) {
            Row(modifier = Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                val expand = stringResource(R.string.player_expand, title, artist)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button, onClick = onExpand)
                        .clearAndSetSemantics {
                            contentDescription = expand
                            role = Role.Button
                        }
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiniBars(
                        if (moving) {
                            shown
                        } else {
                            { ShownVisual.STILL }
                        },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            artist,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = look.lcdMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = time,
                        style = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = look.lcdAccent,
                        maxLines = 1,
                    )
                }
                SmallKey(
                    icon = if (state.showPause) CoreUiR.drawable.ic_pause else CoreUiR.drawable.ic_play,
                    label = stringResource(
                        if (state.showPause) CoreUiR.string.action_pause else CoreUiR.string.action_play,
                    ),
                    onClick = actions.onTogglePlay,
                    tint = look.lcdAccent,
                )
                SmallKey(
                    CoreUiR.drawable.ic_skip_next,
                    stringResource(CoreUiR.string.action_next),
                    actions.onNext,
                    look.lcdText,
                )
            }
        }
    }
}

/** Six bars taken from the spectrum, low to high. */
@Composable
private fun MiniBars(shown: () -> ShownVisual) {
    val accent = lookColors.lcdAccent
    Canvas(Modifier.size(width = 30.dp, height = 22.dp)) {
        val bands = shown().bands
        val gap = 2.dp.toPx()
        val width = (size.width - gap * (MINI_BARS - 1)) / MINI_BARS
        repeat(MINI_BARS) { bar ->
            // Spread over the whole spectrum, so low and high notes both show.
            val band = bands.getOrElse(bar * bands.size / MINI_BARS) { 0f }
            val height = size.height * band.coerceIn(MIN_MINI_BAR, 1f)
            drawRect(accent, Offset(bar * (width + gap), size.height - height), Size(width, height))
        }
    }
}

@Composable
private fun SmallKey(icon: Int, label: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp).padding(1.dp),
            tint = tint,
        )
    }
}

private const val MINI_BARS = 6
private const val MIN_MINI_BAR = 0.1f
