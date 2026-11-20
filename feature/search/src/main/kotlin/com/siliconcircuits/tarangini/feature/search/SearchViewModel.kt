/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.core.domain.repository.SearchRepository
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.component.pickIn
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the search found.
 *
 * @property searched the text the results belong to. It can be older than what is in the field
 * @property results what was found, or null while nothing was searched yet
 * @property failed true when the last search went wrong
 */
internal data class SearchUiState(
    val searched: String = "",
    val results: SearchResults? = null,
    val failed: Boolean = false,
)

/** The search screen: finds songs, albums and artists while the person types. */
@HiltViewModel
internal class SearchViewModel @Inject constructor(
    private val search: SearchRepository,
    private val playback: PlaybackController,
    private val queue: PlaybackQueue,
    private val favourites: FavouriteRepository,
    playlists: PlaylistRepository,
) : ViewModel() {
    /**
     * What is in the search field. A text field needs its text back in the
     * same moment it changes, or typed letters get lost, so this is Compose
     * state and not part of [state].
     */
    var text by mutableStateOf("")
        private set

    private val typed = MutableStateFlow("")
    private val found = MutableStateFlow(SearchUiState())

    val state: StateFlow<SearchUiState> = found.asStateFlow()

    /** A tap plays the song with the songs that were found as the queue, in the order shown. */
    val songActions = SongRowActions(
        onPlay = ::play,
        onPlayNext = { queue.playNext(listOf(it.mediaUri)) },
        onAddToQueue = { queue.addToEnd(listOf(it.mediaUri)) },
        favourites = favourites.songIds().stateIn(viewModelScope, SharingStarted.Eagerly, emptySet()),
        // A mark that cannot be saved leaves the row as it was. That says enough.
        onSetFavourite = { song, favourite ->
            viewModelScope.launch { runCatching { favourites.setFavourite(song.id, favourite) } }
        },
        playlists = playlists.pickIn(viewModelScope),
    )

    init {
        viewModelScope.launch { searchWhileTyping() }
    }

    /** Takes over what the person typed. The search follows after a short pause in typing. */
    fun onTextChange(new: String) {
        text = new.take(MAX_TEXT)
        typed.value = text
    }

    // mapLatest drops the search of a text that was typed over. It is still marked experimental.
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun searchWhileTyping() {
        typed.map { it.trim() }
            .distinctUntilChanged()
            .mapLatest { wanted ->
                if (wanted.none(Char::isLetterOrDigit)) {
                    // Nothing to search for. The old results go at once, not after the pause.
                    SearchUiState()
                } else {
                    delay(TYPING_PAUSE_MS)
                    runCatching { SearchUiState(wanted, search.search(wanted)) }
                        .onFailure { if (it is CancellationException) throw it }
                        .getOrElse { SearchUiState(wanted, failed = true) }
                }
            }
            .collect { found.value = it }
    }

    private fun play(song: Song) {
        val uris = found.value.results?.songs.orEmpty().map { it.mediaUri }
        val index = uris.indexOf(song.mediaUri)
        if (index >= 0) playback.play(uris, index) else playback.play(listOf(song.mediaUri))
    }

    companion object {
        /**
         * The pause in typing before a search starts. Results must be there
         * 300 ms after the last key, and a search takes a few tens of them.
         */
        const val TYPING_PAUSE_MS = 200L

        /** The search itself reads no more than this many characters. */
        private const val MAX_TEXT = 256
    }
}
