/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistFiles
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedList
import com.siliconcircuits.tarangini.feature.library.list.attempt
import com.siliconcircuits.tarangini.feature.library.navigation.PlaylistRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Where a place of a playlist can be moved to from its menu. */
internal enum class PlaylistMove { UP, DOWN, TOP, END }

/**
 * One playlist: its name and its songs in order.
 *
 * @property name the name now; it changes after a rename
 * @property isGone true once the playlist was removed, here or elsewhere; the screen then closes
 */
internal data class PlaylistUiState(val name: String, val isGone: Boolean = false)

@HiltViewModel
internal class PlaylistViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val playlists: PlaylistRepository,
    library: LibraryRepository,
    private val playback: PlaybackController,
    files: PlaylistFiles,
) : ViewModel() {
    private val playlistId: Long = checkNotNull(saved[PlaylistRoute.PLAYLIST_ID])

    /** Writes this playlist to a file. */
    val export = PlaylistFileExport(viewModelScope, files, playlistId)
    private val shown = MutableStateFlow(PlaylistUiState(saved.get<String>(PlaylistRoute.NAME).orEmpty()))

    val state: StateFlow<PlaylistUiState> = shown.asStateFlow()

    val songs = PagedList(viewModelScope, PlaylistSong::itemId) { playlists.songs(playlistId, it) }

    init {
        // A rescan can take the file of a song away or bring it back.
        viewModelScope.launch {
            merge(playlists.changes(), library.changes()).collect {
                attempt {
                    val now = playlists.playlists().firstOrNull { it.id == playlistId }
                    shown.value = PlaylistUiState(now?.name ?: shown.value.name, isGone = now == null)
                    songs.refresh()
                }
            }
        }
    }

    fun rename(name: String) = change { playlists.rename(playlistId, name) }

    fun delete() = change { playlists.delete(playlistId) }

    fun remove(place: PlaylistSong) = change { playlists.remove(place.itemId) }

    /** Moves a place among the rows that are on screen. */
    fun move(place: PlaylistSong, to: PlaylistMove) {
        val shown = songs.state.value
        val rows = shown.items
        val at = rows.indexOfFirst { it.itemId == place.itemId }
        // Past the rows read so far the next place is not known, so nothing moves there.
        val canGoDown = at < rows.lastIndex && (at + 2 <= rows.lastIndex || !shown.hasMore)
        val allowed = when (to) {
            PlaylistMove.UP -> at > 0
            PlaylistMove.DOWN -> at >= 0 && canGoDown
            PlaylistMove.TOP, PlaylistMove.END -> at >= 0
        }
        val before = when (to) {
            PlaylistMove.UP -> rows.getOrNull(at - 1)
            PlaylistMove.DOWN -> rows.getOrNull(at + 2)
            PlaylistMove.TOP -> rows.firstOrNull()
            PlaylistMove.END -> null
        }
        if (allowed) change { playlists.move(playlistId, place.itemId, before?.itemId) }
    }

    /** Plays the playlist in its order from [from] on, or from its start. */
    fun play(from: PlaylistSong? = null) = start(shuffled = false, from)

    /** Plays the playlist in a new order each time. */
    fun shuffle() = start(shuffled = true, from = null)

    private fun start(shuffled: Boolean, from: PlaylistSong?) {
        viewModelScope.launch {
            val all = attempt { wholeList() }.getOrDefault(emptyList())
            val order = if (shuffled) all.shuffled() else all
            val index = order.indexOfFirst { it.itemId == from?.itemId }.coerceAtLeast(0)
            if (order.isNotEmpty()) playback.play(order.map { it.song.mediaUri }, index)
        }
    }

    private suspend fun wholeList(): List<PlaylistSong> {
        val all = mutableListOf<PlaylistSong>()
        do {
            val page = playlists.songs(playlistId, Page(Page.MAX_LIMIT, all.size))
            all += page
        } while (page.size == Page.MAX_LIMIT)
        return all
    }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch { attempt { block() } }
    }
}
