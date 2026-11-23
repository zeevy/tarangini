/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.feature.player.nowplaying.CompactSong
import com.siliconcircuits.tarangini.feature.player.showsPause
import com.siliconcircuits.tarangini.feature.player.togglePlay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The mini player: the current item with play or pause and next, on every screen. */
@HiltViewModel
internal class MiniPlayerViewModel @Inject constructor(private val playback: PlaybackController) : ViewModel() {
    /**
     * What to show, or null when nothing is loaded and the card is hidden. The player is watched
     * only while the card is on screen, and a short time after, so a turn of the phone does not
     * start again.
     */
    val state: StateFlow<CompactSong?> = playback.state
        .map { it.toUiState() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), playback.state.value.toUiState())

    /** Pauses when play was asked for, else goes on with the current item. */
    fun togglePlay() {
        playback.togglePlay()
    }

    fun next() = playback.next()

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}

private fun PlaybackState.toUiState(): CompactSong? {
    val item = currentItem ?: return null
    return CompactSong(
        title = item.title,
        artist = item.artist,
        // Whole seconds: the card shows no more, and every tick of the position would draw it again.
        positionMs = positionMs / MS_PER_SECOND * MS_PER_SECOND,
        durationMs = durationMs?.takeIf { it > 0 },
        showPause = showsPause,
        // A paused stream that still fills its buffer is at rest for the person.
        isBuffering = isBuffering && showsPause,
        hasError = error != null,
        isStream = item.isStream,
    )
}

private const val MS_PER_SECOND = 1_000L
