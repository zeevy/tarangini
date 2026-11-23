/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.model.checkPlaylistName
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The queue as the screen shows it.
 *
 * @property items the items in the order of the queue
 * @property currentIndex the place of the item that plays, or null when the queue is empty
 * @property playOrder with shuffle on, the places from the item that plays on, in the order they play
 */
internal data class QueueUiState(
    val items: List<PlaybackItem> = emptyList(),
    val currentIndex: Int? = null,
    val playOrder: List<Int>? = null,
)

/** A song taken out of Up next that comes back with Undo until the time for Undo has run out. */
internal data class PendingRemoval(val index: Int, val itemId: String, val title: String?)

/** What a tool of Up next did, for a short message at the foot of the screen. */
internal sealed interface QueueNotice {
    /** The queue was saved as the playlist [name]. */
    data class Saved(val name: String) : QueueNotice

    /** The queue could not be saved: no song of it is in the library, or the name is taken. */
    data object NotSaved : QueueNotice
}

/** Up next: what plays after the current song, in an order the person can change. */
@HiltViewModel
internal class QueueViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val queue: PlaybackQueue,
    private val playlists: PlaylistRepository,
    private val songs: SongLookup,
    private val library: LibraryRepository,
) : ViewModel() {
    val state: StateFlow<QueueUiState> = playback.state
        .map { QueueUiState(it.queue, it.currentIndex, it.playOrder) }
        // The player reports its place several times a second with the same list. Comparing a list
        // of many thousand items each time would cost more than drawing it.
        .distinctUntilChanged { old, new ->
            old.items === new.items && old.currentIndex == new.currentIndex && old.playOrder == new.playOrder
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_AFTER_MS),
            playback.state.value.let { QueueUiState(it.queue, it.currentIndex, it.playOrder) },
        )

    private val removal = MutableStateFlow<PendingRemoval?>(null)
    private var removalTimer: Job? = null
    private var shuffling: Job? = null

    /** The song that waits to be taken out. Up next hides its row and offers Undo. */
    val pendingRemoval: StateFlow<PendingRemoval?> = removal.asStateFlow()

    private val notices = MutableSharedFlow<QueueNotice>(extraBufferCapacity = 1)

    /** What the last tool did. */
    val notice: SharedFlow<QueueNotice> = notices

    fun playAt(index: Int) = queue.playAt(index)

    fun move(from: Int, to: Int) = queue.move(from, to)

    /**
     * Hides the item at [index] at once and takes it out of the queue when the time for Undo ends.
     * A second remove first takes out the one before it.
     */
    fun remove(index: Int) {
        val item = state.value.items.getOrNull(index) ?: return
        commitRemoval()
        removal.value = PendingRemoval(index, item.id, item.title)
        removalTimer = viewModelScope.launch {
            delay(UNDO_MS)
            commitRemoval()
        }
    }

    /** Puts the hidden song back: it was never taken out of the queue. */
    fun undoRemove() {
        removalTimer?.cancel()
        removal.value = null
    }

    /** Takes every song out but the one that plays. */
    fun clear() {
        commitRemoval()
        queue.keepOnlyCurrent()
    }

    /** Saves the songs of the queue, in its order, as a new playlist named [typed]. */
    fun saveAsPlaylist(typed: String) {
        val name = checkPlaylistName(typed) ?: return
        val ids = playback.state.value.queue.map { it.id }
        viewModelScope.launch {
            val saved = attempt {
                val found = songs.byMediaUri(ids)
                val songIds = ids.mapNotNull { found[it]?.id }
                if (songIds.isEmpty()) return@attempt false
                val playlist = playlists.create(name) ?: return@attempt false
                playlists.addAll(playlist, songIds)
                true
            }
            notices.tryEmit(if (saved == true) QueueNotice.Saved(name) else QueueNotice.NotSaved)
        }
    }

    /** Plays every song of the library in a new order. */
    fun shuffleAll() {
        // A second tap while the songs are read starts nothing more.
        if (shuffling?.isActive == true) return
        shuffling = viewModelScope.launch {
            val all = attempt { allSongUris() }.orEmpty()
            if (all.isNotEmpty()) playback.play(all.shuffled())
        }
    }

    override fun onCleared() {
        // The time for Undo ends with the screen.
        commitRemoval()
    }

    private suspend fun allSongUris(): List<String> {
        val all = mutableListOf<String>()
        do {
            val page = library.songs(SongSort.TITLE, Page(Page.MAX_LIMIT, all.size))
            all += page.map { it.mediaUri }
        } while (page.size == Page.MAX_LIMIT)
        return all
    }

    // The queue may have changed while Undo was offered: the item is found again by its id, at its
    // old place when it is still there.
    private fun commitRemoval() {
        removalTimer?.cancel()
        val pending = removal.value ?: return
        removal.value = null
        // The live queue of the player: the screen state can be old once nobody has watched it.
        val items = playback.state.value.queue
        val index = if (items.getOrNull(pending.index)?.id == pending.itemId) {
            pending.index
        } else {
            items.indexOfFirst { it.id == pending.itemId }
        }
        if (index >= 0) queue.remove(index)
    }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
        const val UNDO_MS = 4_000L
    }
}

private suspend fun <T> attempt(block: suspend () -> T): T? = runCatching { block() }
    .onFailure { if (it is CancellationException) throw it }
    .getOrNull()
