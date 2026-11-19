/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.resume

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.ResumeStore
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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
 * Everything here runs on the main thread, like the player.
 *
 * @param scope lives longer than the service, so the last save is not lost when the service ends
 */
internal class PlaybackResume(
    private val player: Player,
    private val store: ResumeStore,
    private val scope: CoroutineScope,
) : Player.Listener {
    /** Wakes the one coroutine that saves. A wake that comes while it works is kept, more of them count as one. */
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var saved: Deferred<SavedPlayback?>? = null
    private var queueChanged = false
    private var latestPlace: ResumePlace? = null

    /** The ids of the queue that is stored, to leave out a save of the same queue. */
    private var storedIds: List<String>? = null
    private var ended = false

    /**
     * Reads what was saved and puts it into the player, paused and not
     * prepared: nothing sounds and no notification shows until somebody
     * presses play. Saving starts only after that, so the empty player of a
     * new service never wipes the stored queue.
     */
    fun start() {
        val load = scope.async(start = CoroutineStart.LAZY) { store.load() }
        saved = load
        scope.launch {
            val playback = load.await()
            if (ended) return@launch
            storedIds = playback?.mediaIds
            if (playback != null && player.mediaItemCount == 0) player.fill(playback)
            player.addListener(this@PlaybackResume)
            launch { saveWhenWoken() }
            launch { wakeWhilePlaying() }
        }
    }

    /**
     * What the session plays when the system, a Bluetooth device or the car
     * asks to go on and the player is empty, or null when nothing was saved.
     */
    // The answer class is API that Media3 may still change.
    @OptIn(UnstableApi::class)
    suspend fun resumption(): MediaItemsWithStartPosition? = saved?.await()?.toResumption()

    /** Saves the place a last time and stops. Call it before the player is released. */
    fun end() {
        if (ended) return
        ended = true
        player.removeListener(this)
        if (storedIds != null) {
            latestPlace = player.place()
            wake.trySend(Unit)
        }
        // The saving coroutine takes what is waiting and ends by itself.
        wake.close()
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_TIMELINE_CHANGED)) queueChanged = true
        if (events.movesThePlace()) {
            latestPlace = player.place()
            wake.trySend(Unit)
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // The restored queue is not prepared. A plain "play" from the car or a headset must still start it.
        if (playWhenReady && player.playbackState == Player.STATE_IDLE && player.playerError == null) player.prepare()
    }

    private suspend fun saveWhenWoken() {
        // The loop ends when the channel is closed and what waited in it is saved.
        while (wake.receiveCatching().isSuccess) {
            if (queueChanged && !ended) {
                // More parts of a long list may follow at once.
                delay(QUEUE_STILL)
            }
            if (queueChanged && !ended) {
                queueChanged = false
                saveQueue()
            } else {
                latestPlace?.let { store.savePlace(it) }
            }
        }
    }

    private suspend fun saveQueue() {
        val ids = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId }
        val place = player.place()
        if (ids == storedIds) {
            store.savePlace(place)
        } else {
            store.saveQueue(SavedPlayback(ids, place))
            storedIds = ids
        }
    }

    private suspend fun wakeWhilePlaying() {
        while (!ended) {
            delay(SAVE_EVERY)
            if (!ended && player.isPlaying) {
                latestPlace = player.place()
                wake.trySend(Unit)
            }
        }
    }

    private companion object {
        val SAVE_EVERY = 5.seconds
        val QUEUE_STILL = 1.seconds
    }
}
