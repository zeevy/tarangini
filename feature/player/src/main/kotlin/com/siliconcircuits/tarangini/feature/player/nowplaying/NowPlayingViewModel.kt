/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.model.SongFormat
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.domain.playback.SleepTimer
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import com.siliconcircuits.tarangini.core.ui.component.PlaylistPick
import com.siliconcircuits.tarangini.core.ui.component.pickIn
import com.siliconcircuits.tarangini.feature.player.showsPause
import com.siliconcircuits.tarangini.feature.player.togglePlay
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What the now playing screen shows.
 *
 * @property showPause true when the play button pauses
 * @property positionMs how far the item has played. After a seek it is the place asked for, until
 *   the player is there
 * @property durationMs the length of the item, or null for a live stream, which has no seek bar
 * @property songId the library row of the song that plays, or null for an item that is no song of the library
 * @property format how the item is stored, or null until the player has opened it
 * @property place the place of the item in the queue, counted from 1
 * @property queueSize how many items the queue holds
 * @property error why the item does not play, or null. For a stream it is set only after the player
 *   has given up trying
 */
internal data class NowPlayingUiState(
    val title: String?,
    val artist: String?,
    val album: String?,
    val artworkUri: String?,
    val showPause: Boolean,
    val isBuffering: Boolean,
    val positionMs: Long,
    val durationMs: Long?,
    val shuffle: Boolean,
    val repeat: RepeatMode,
    val error: PlaybackError? = null,
    val isFavourite: Boolean? = null,
    val songId: Long? = null,
    val speed: Float = 1f,
    val sleep: SleepState = SleepState.Off,
    val format: SongFormat? = null,
    val place: Int = 1,
    val queueSize: Int = 1,
) {
    val isLive: Boolean get() = durationMs == null
}

/** The now playing screen: the current item with every control of the player. */
@HiltViewModel
internal class NowPlayingViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val queue: PlaybackQueue,
    private val favourites: FavouriteRepository,
    private val songs: SongLookup,
    playlists: PlaylistRepository,
    sleep: SleepTimer,
) : ViewModel() {
    /** What "Add to playlist" needs. */
    val playlistPick: PlaylistPick = playlists.pickIn(viewModelScope)

    /** The speed and the sleep timer. They belong to the player, so they stay when this screen closes. */
    val options = PlaybackOptionActions(
        onSetSpeed = playback::setSpeed,
        onSleepAfter = sleep::stopAfter,
        onSleepAtEnd = sleep::stopAtEndOfTrack,
        onCancelSleep = sleep::cancel,
    )

    /**
     * The place the last seek asked for, while the player is not there yet. The player goes on
     * reporting the old place for a moment after a seek. Shown as it is, the thumb of the seek bar
     * would jump back to the old place and then forward again.
     */
    private val seekTarget = MutableStateFlow<SeekTarget?>(null)
    private var seekWait: Job? = null

    /** Whether the song that plays is a favourite. Null for an item that is no song of the library. */
    private val favourite = MutableStateFlow<FavouriteMark?>(null)

    /** What to show, or null when nothing is loaded. */
    val state: StateFlow<NowPlayingUiState?> =
        combine(playback.state, seekTarget, favourite, sleep.state) { now, target, mark, timer ->
            now.toUiState(target, mark)?.copy(sleep = timer)
        }
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_AFTER_MS),
                playback.state.value.toUiState(null, null),
            )

    init {
        viewModelScope.launch { watchFavourite() }
    }

    fun togglePlay() = playback.togglePlay()

    fun next() = playback.next()

    fun previous() = playback.previous()

    /** Jumps to [positionMs] of the current item. */
    fun seekTo(positionMs: Long) {
        val before = playback.state.value
        val itemId = before.currentItem?.id ?: return
        seekTarget.value = SeekTarget(itemId, positionMs)
        playback.seekTo(positionMs)
        seekWait?.cancel()
        seekWait = viewModelScope.launch {
            // The wait has an end: a seek the player refuses must not freeze the seek bar. The place
            // must also have changed, or a short seek would be taken as done before it started.
            withTimeoutOrNull(SEEK_WAIT_MS) {
                playback.state.first {
                    it.positionMs != before.positionMs && abs(it.positionMs - positionMs) < NEAR_MS
                }
            }
            seekTarget.value = null
        }
    }

    fun toggleShuffle() = queue.setShuffle(!playback.state.value.shuffle)

    /** Goes to the next repeat mode: off, all, one, and off again. */
    fun cycleRepeat() = queue.setRepeat(
        when (playback.state.value.repeat) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        },
    )

    /** Marks the song that plays as a favourite, or takes the mark away. */
    fun toggleFavourite() {
        val songId = favourite.value?.songId ?: return
        // The mark is read again here, so two quick taps are two changes. A mark that cannot be
        // saved leaves the heart as it was. That says enough.
        viewModelScope.launch {
            runCatching { favourites.setFavourite(songId, songId !in favourites.songIds().first()) }
        }
    }

    private suspend fun watchFavourite() {
        val song = playback.state.map { it.currentItem?.id }.distinctUntilChanged().map { id -> id to songIdOf(id) }
        combine(song, favourites.songIds()) { (itemId, songId), ids ->
            if (itemId != null && songId != null) FavouriteMark(itemId, songId, songId in ids) else null
        }.collect { favourite.value = it }
    }

    /** The library row of what plays, or null for a stream and for a file the library does not know. */
    private suspend fun songIdOf(itemId: String?): Long? = itemId?.let { id ->
        runCatching { songs.byMediaUri(listOf(id))[id]?.id }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()
    }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
        const val SEEK_WAIT_MS = 2_000L
        const val NEAR_MS = 1_500L
    }
}

/** The place a seek asked for, with the item it was asked in. */
private data class SeekTarget(val itemId: String, val positionMs: Long)

private data class FavouriteMark(val itemId: String, val songId: Long, val on: Boolean)

private fun PlaybackState.toUiState(seekTarget: SeekTarget?, mark: FavouriteMark?): NowPlayingUiState? {
    val item = currentItem ?: return null
    val length = durationMs?.takeIf { it > 0 }
    // A place asked for in one item says nothing about the next one.
    val position = seekTarget?.takeIf { it.itemId == item.id }?.positionMs ?: positionMs
    return NowPlayingUiState(
        title = item.title,
        artist = item.artist,
        album = item.album,
        artworkUri = item.artworkUri,
        showPause = showsPause,
        isBuffering = isBuffering && showsPause,
        positionMs = if (length == null) position else position.coerceIn(0, length),
        durationMs = length,
        shuffle = shuffle,
        repeat = repeat,
        error = error,
        // A mark read for one item says nothing about the next one.
        isFavourite = mark?.takeIf { it.itemId == item.id }?.on,
        songId = mark?.takeIf { it.itemId == item.id }?.songId,
        speed = speed,
        format = format,
        place = (currentIndex ?: 0) + 1,
        queueSize = queue.size.coerceAtLeast(1),
    )
}
