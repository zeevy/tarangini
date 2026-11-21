/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.repository.SongLyrics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** The lyrics of the song that plays, read from the tags of its file. */
@HiltViewModel
internal class LyricsViewModel @Inject constructor(playback: PlaybackController, lyrics: SongLyrics) : ViewModel() {
    /**
     * The lyrics, or null while they are read and when the song has none. The file is read once
     * for each song, not at every change of the position. A new song ends the reading of the old
     * one and takes its lyrics away at once, so they never show under the title of the next song.
     */
    // transformLatest is marked experimental. It is what drops the reading of a song that was skipped.
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<String?> = playback.state
        .map { it.currentItem?.id }
        .distinctUntilChanged()
        .transformLatest { address ->
            emit(null)
            if (address != null) emit(lyrics.lyricsOf(address))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), null)

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}
