/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumArt
import com.siliconcircuits.tarangini.core.ui.theme.PanelColor
import com.siliconcircuits.tarangini.core.ui.theme.PanelEdge
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.player.ItemTexts
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The mini player: the item that plays or is paused, with play or pause and
 * next. It shows nothing while the player has nothing loaded.
 *
 * @param onOpen opens the now playing screen
 */
@Composable
fun MiniPlayer(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    WatchedMiniPlayer(onOpen = onOpen, modifier = modifier)
}

@Composable
private fun WatchedMiniPlayer(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { MiniPlayerBar(it, viewModel::togglePlay, viewModel::next, onOpen, modifier) }
}

@Composable
internal fun MiniPlayerBar(
    state: MiniPlayerUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A pill in the card colour with space around it.
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.medium, vertical = Spacing.small),
        shape = CircleShape,
        color = PanelColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = PanelEdge,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable(
                        onClickLabel = stringResource(R.string.mini_player_open),
                        role = Role.Button,
                        onClick = onOpen,
                    )
                    .padding(start = Spacing.medium, top = Spacing.small, bottom = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(state.artworkUri)
                ItemTexts(
                    state.title,
                    state.artist,
                    Modifier.weight(1f).padding(horizontal = Spacing.medium),
                    state.isStream,
                )
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        painter = painterResource(
                            if (state.showPause) CoreUiR.drawable.ic_pause else CoreUiR.drawable.ic_play,
                        ),
                        contentDescription = stringResource(
                            if (state.showPause) CoreUiR.string.action_pause else CoreUiR.string.action_play,
                        ),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_skip_next),
                        contentDescription = stringResource(CoreUiR.string.action_next),
                    )
                }
            }
            ProgressLine(state)
        }
    }
}

/**
 * The thin line at the bottom of the bar. It moves while the player waits for data,
 * shows how far the item has played when its length is known, and is an
 * empty space of the same height otherwise, so the bar never changes height.
 */
@Composable
private fun ProgressLine(state: MiniPlayerUiState) {
    // The line stays inside the straight part of the pill, clear of its round ends.
    val line = Modifier.fillMaxWidth().padding(horizontal = Spacing.large).height(LINE_HEIGHT)
    when {
        state.isBuffering -> {
            val loading = stringResource(R.string.player_loading)
            LinearProgressIndicator(
                modifier = line.semantics { contentDescription = loading },
                trackColor = Color.Transparent,
            )
        }

        state.progress != null -> LinearProgressIndicator(
            progress = { state.progress },
            modifier = line,
            trackColor = Color.Transparent,
            // The line has no gap and no dot at its end: it is a thin edge of the bar, not a control.
            gapSize = 0.dp,
            drawStopIndicator = {},
        )

        else -> Box(line)
    }
}

private val LINE_HEIGHT = 2.dp
