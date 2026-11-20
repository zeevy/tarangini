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
import com.siliconcircuits.tarangini.feature.player.showsPause
import com.siliconcircuits.tarangini.feature.player.togglePlay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * What the mini player shows.
 *
 * @property title the title of the current item, or null when the player does not know it
 * @property showPause true when the button pauses. It is true from the tap on play, also while
 *   the player still loads, so a second tap pauses. It is false after an error and at the end of
 *   the queue
 * @property isBuffering true while the player waits for data
 * @property progress how much of the item has played, from 0 to 1, or null when its length is
 *   not known, as with live radio
 */
internal data class MiniPlayerUiState(
    val title: String?,
    val artist: String?,
    val artworkUri: String?,
    val showPause: Boolean,
    val isBuffering: Boolean,
    val progress: Float?,
    val isStream: Boolean = false,
)

/** The mini player: the current item with play or pause and next, on every screen. */
@HiltViewModel
internal class MiniPlayerViewModel @Inject constructor(private val playback: PlaybackController) : ViewModel() {
    /**
     * What to show, or null when nothing is loaded and the bar is hidden. The player is watched
     * only while the bar is on screen, and a short time after, so a turn of the phone does not
     * start again.
     */
    val state: StateFlow<MiniPlayerUiState?> = playback.state
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

private fun PlaybackState.toUiState(): MiniPlayerUiState? {
    val item = currentItem ?: return null
    val length = durationMs?.takeIf { it > 0 }
    return MiniPlayerUiState(
        title = item.title,
        artist = item.artist,
        artworkUri = item.artworkUri,
        isStream = item.isStream,
        showPause = showsPause,
        // A paused stream that still fills its buffer is at rest for the person.
        isBuffering = isBuffering && showsPause,
        // The line is 2 dp high and at most a screen wide, so steps of a thousandth are enough.
        // Without the rounding every tick of the position would draw the bar again.
        progress = length?.let {
            (positionMs * PROGRESS_STEPS / it).coerceIn(0, PROGRESS_STEPS) /
                PROGRESS_STEPS.toFloat()
        },
    )
}

private const val PROGRESS_STEPS = 1_000L
