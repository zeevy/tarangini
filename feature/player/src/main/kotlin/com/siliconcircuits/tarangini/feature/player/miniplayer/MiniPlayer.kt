/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.player.R
import com.siliconcircuits.tarangini.feature.player.nowplaying.CompactCard
import com.siliconcircuits.tarangini.feature.player.nowplaying.CompactSong
import com.siliconcircuits.tarangini.feature.player.nowplaying.ShownVisual
import com.siliconcircuits.tarangini.feature.player.nowplaying.VisualViewModel

/**
 * The mini player: the compact display card with the item that plays or is paused, its bars,
 * the time, play or pause and next. It shows nothing while the player has nothing loaded.
 *
 * @param onOpen opens the player tab
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
    visualViewModel: VisualViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shownState = visualViewModel.shown.collectAsStateWithLifecycle()
    val moving = state?.isMoving == true
    LaunchedEffect(moving) { visualViewModel.setMoving(moving) }
    val shown = remember(shownState) { { shownState.value } }
    state?.let { MiniPlayerBar(it, viewModel::togglePlay, viewModel::next, onOpen, modifier, shown) }
}

/** The card of the mini player, with space around it. */
@Composable
internal fun MiniPlayerBar(
    state: CompactSong,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    shown: () -> ShownVisual = { ShownVisual.STILL },
) {
    CompactCard(
        song = state,
        onTogglePlay = onTogglePlay,
        onNext = onNext,
        shown = shown,
        onOpen = onOpen,
        openLabel = R.string.mini_player_open_song,
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.small, vertical = Spacing.small / 2),
    )
}
