/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlayHistory
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.component.pickIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What Home shows. A section with nothing in it is not shown.
 *
 * @property isLoading true until the lists were read once
 * @property paused the item to go on with: what is loaded in the player while it does not play
 * @property recent the songs heard last, the newest first
 * @property playlists the playlists by name
 */
internal data class HomeUiState(
    val isLoading: Boolean = true,
    val paused: PlaybackItem? = null,
    val recent: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
) {
    /** True on a new install: nothing was heard and nothing was made yet. */
    val isFirstUse: Boolean get() = !isLoading && paused == null && recent.isEmpty() && playlists.isEmpty()
}

/** Home: a way back to what was heard last and to the playlists. */
@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val queue: PlaybackQueue,
    private val history: PlayHistory,
    private val playlists: PlaylistRepository,
    private val favourites: FavouriteRepository,
    library: LibraryRepository,
) : ViewModel() {
    private val lists = MutableStateFlow<Lists?>(null)

    val state: StateFlow<HomeUiState> = combine(playback.state, lists) { now, read ->
        HomeUiState(
            isLoading = read == null,
            paused = now.currentItem.takeUnless { now.isPlaying || now.playWhenReady },
            recent = read?.recent.orEmpty(),
            playlists = read?.playlists.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), HomeUiState())

    /** A tap on a recent song plays the recent songs from that song on. */
    val songActions = SongRowActions(
        onPlay = ::play,
        onPlayNext = { queue.playNext(listOf(it.mediaUri)) },
        onAddToQueue = { queue.addToEnd(listOf(it.mediaUri)) },
        favourites = favourites.songIds().stateIn(viewModelScope, SharingStarted.Eagerly, emptySet()),
        onSetFavourite = { song, on -> viewModelScope.launch { attempt { favourites.setFavourite(song.id, on) } } },
        playlists = playlists.pickIn(viewModelScope),
    )

    init {
        // A counted play, a changed playlist and a rescan each change what Home lists.
        viewModelScope.launch {
            merge(history.changes(), playlists.changes(), library.changes()).conflate().collect { read() }
        }
    }

    /** Goes on with the item that is loaded. */
    fun resume() = playback.resume()

    private fun play(song: Song) {
        val uris = lists.value?.recent.orEmpty().map { it.mediaUri }
        val index = uris.indexOf(song.mediaUri)
        if (index >= 0) playback.play(uris, index) else playback.play(listOf(song.mediaUri))
    }

    /**
     * A list that cannot be read is shown as it was before. While nothing could be read yet Home
     * stays empty: the welcome text is only for a library that was read and has nothing.
     */
    private suspend fun read() {
        val before = lists.value
        val recent = attempt { history.recentSongs(RECENT_SONGS) } ?: before?.recent
        val made = attempt { playlists.playlists().take(PLAYLISTS) } ?: before?.playlists
        if (recent != null && made != null) lists.value = Lists(recent, made)
    }

    private data class Lists(val recent: List<Song>, val playlists: List<Playlist>)

    private companion object {
        const val STOP_AFTER_MS = 5_000L

        /** Home is a start page, not the whole list: the rest is in the Library. */
        const val RECENT_SONGS = 10
        const val PLAYLISTS = 10
    }
}

/** The result, or null after a failure. A cancelled coroutine stays cancelled. */
private suspend fun <T> attempt(block: suspend () -> T): T? =
    runCatching { block() }.onFailure { if (it is CancellationException) throw it }.getOrNull()
