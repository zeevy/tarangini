/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.resume

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.playback.ResumeStore
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback
import com.siliconcircuits.tarangini.core.playback.controller.toPlaylistMetadata
import com.siliconcircuits.tarangini.core.playback.mapper.playableItem
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brings back what was playing when the playback service starts, and saves
 * the queue and the place in it while the service runs.
 *
 * The place is saved on a pause, a change of item, a seek and every few
 * seconds while something plays. The queue is saved when it changed and has
 * been still for a moment, because a long list comes into the player in
 * parts. Saves run one after the other, so an old save never lands on a
 * newer one.
 *
 * A read or a save that the database refuses is left out: the music must
 * not stop because the place could not be stored.
 *
 * Everything here runs on the main thread, like the player.
 *
 * @param scope lives longer than the service, so the last save is not lost when the service ends
 * @param texts gives the items of the queue their title, artist and album
 */
internal class PlaybackResume(
    private val player: Player,
    private val store: ResumeStore,
    private val scope: CoroutineScope,
    private val texts: suspend (List<MediaItem>) -> List<MediaItem> = { it },
) : Player.Listener {
    /** Wakes the one coroutine that saves. A wake that comes while it works is kept, more of them count as one. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** Counts the changes of the queue. The queue is stored when [savedChange] is the same number. */
    private var queueChange = 0
    private var savedChange = 0

    /** The ids of the queue that is stored, to leave out a save of the same queue. */
    private var storedIds: List<String>? = null

    /** The source of the queue that is stored. A new source for the same songs is saved as a new queue. */
    private var storedSource: QueueSource? = null

    /** What [end] read from the player, for the last save after the player is released. */
    private var atEnd: SavedPlayback? = null
    private var ticker: Job? = null
    private var ended = false

    /**
     * Reads what was saved and puts it into the player, paused and not
     * prepared: nothing sounds and no notification shows until somebody
     * presses play. Saving starts only after that, so the empty player of a
     * new service never wipes the stored queue.
     */
    fun start() {
        scope.launch {
            val playback = store.loadOrNull()
            val items = playback?.items()
            if (ended) return@launch
            if (player.mediaItemCount == 0) {
                storedIds = playback?.mediaIds
                storedSource = playback?.source
                if (playback != null && items != null) player.fill(items, playback.place, playback.source)
            } else {
                // Somebody filled the player during the read. That queue is the one to store.
                queueChange++
                wake.trySend(Unit)
            }
            player.addListener(this@PlaybackResume)
            launch { saveWhenWoken() }
            ticker = launch { wakeWhilePlaying() }
        }
    }

    /**
     * What the session plays when the system, a Bluetooth device or the car
     * asks to go on and the player is empty, or null when nothing was saved.
     * It is read again each time, so a queue that was emptied since the
     * service started does not come back.
     */
    // The answer class is API that Media3 may still change.
    @OptIn(UnstableApi::class)
    suspend fun resumption(): MediaItemsWithStartPosition? = store.loadOrNull()?.let { saved ->
        val items = saved.items()
        // Media3 puts the items into the player but knows nothing of the list they came from.
        player.playlistMetadata = saved.source.toPlaylistMetadata()
        MediaItemsWithStartPosition(items, saved.place.index, saved.place.positionMs)
    }

    private suspend fun SavedPlayback.items(): List<MediaItem> = texts(mediaIds.map(::playableItem))

    /** Saves the queue and the place a last time and stops. Call it before the player is released. */
    fun end() {
        if (ended) return
        ended = true
        player.removeListener(this)
        ticker?.cancel()
        if (queueChange != savedChange || storedIds != null) atEnd = player.playback()
        // The saving coroutine ends its work, saves what was read here and ends by itself.
        wake.close()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        val newQueue = events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_PLAYLIST_METADATA_CHANGED)
        if (newQueue) queueChange++
        if (newQueue || events.movesThePlace()) wake.trySend(Unit)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // The restored queue is not prepared. A plain "play" from the car or a headset must still start it.
        if (playWhenReady && player.playbackState == Player.STATE_IDLE && player.playerError == null) player.prepare()
    }

    private suspend fun saveWhenWoken() {
        // The loop ends when the channel is closed.
        while (wake.receiveCatching().isSuccess) {
            if (queueChange != savedChange) {
                saveQueueWhenStill()
            } else if (!ended) {
                val place = player.place()
                store.writing { savePlace(place) }
            }
        }
        atEnd?.let { save(it) }
    }

    private suspend fun saveQueueWhenStill() {
        while (!ended && queueChange != savedChange) {
            val seen = queueChange
            // More parts of a long list may follow at once.
            delay(QUEUE_STILL)
            if (ended || seen != queueChange) continue
            // A save that failed is tried again with the next wake.
            if (!save(player.playback())) return
            savedChange = seen
        }
    }

    private suspend fun save(playback: SavedPlayback): Boolean = store.writing {
        if (playback.mediaIds == storedIds && playback.source == storedSource) {
            savePlace(playback.place)
        } else {
            saveQueue(playback)
            storedIds = playback.mediaIds
            storedSource = playback.source
        }
    }

    private suspend fun wakeWhilePlaying() {
        while (true) {
            delay(SAVE_EVERY)
            if (player.isPlaying) wake.trySend(Unit)
        }
    }

    private companion object {
        val SAVE_EVERY = 5.seconds
        val QUEUE_STILL = 1.seconds
    }
}

/** What was saved, or null when nothing was saved or the database cannot be read. */
private suspend fun ResumeStore.loadOrNull(): SavedPlayback? =
    runCatching { load() }.onFailure { if (it is CancellationException) throw it }.getOrNull()

/** Runs [write] and says whether it worked. A database that refuses a write must not end the app. */
private suspend fun ResumeStore.writing(write: suspend ResumeStore.() -> Unit): Boolean =
    runCatching { write() }.onFailure { if (it is CancellationException) throw it }.isSuccess
