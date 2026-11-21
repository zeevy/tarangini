/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.playback.di.PlaybackCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val POSITION_STEP = 500.milliseconds

/**
 * The playback controller of the phone UI. It shows what the player of the media session does
 * and passes commands to it. [PlayerLink] keeps the connection.
 *
 * The scope it is given must run on the main thread: a media controller may be used only there.
 */
@Singleton
internal class SessionPlaybackController @Inject constructor(
    private val link: PlayerLink,
    private val loader: QueueLoader,
    @param:PlaybackCoroutineScope private val scope: CoroutineScope,
) : PlaybackController {
    private val current = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = current.asStateFlow()

    private var shown: Player? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // A long queue is costly to read, so it is read again only when it may have changed.
            val queueChanged = events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_MEDIA_METADATA_CHANGED,
            )
            current.value = player.toPlaybackState(knownQueue = current.value.queue.takeIf { !queueChanged })
        }
    }

    init {
        link.onPlayerChanged = ::follow
        follow(link.player)
        scope.launch { current.followPositionWhileWatched { shown } }
    }

    override fun play(mediaUris: List<String>, startIndex: Int) = loader.replace(mediaUris, startIndex)

    // The words go to the session as a search request, the way a voice assistant sends them, so a
    // request spoken on the phone and one spoken in the car are answered by the same code.
    override fun playSpoken(words: String) = send { player ->
        val request = MediaItem.RequestMetadata.Builder().setSearchQuery(words).build()
        // The session starts to play when the words found something. A "play" from here would also
        // start the old queue when they found nothing.
        player.setMediaItem(MediaItem.Builder().setRequestMetadata(request).build())
    }

    override fun resume() = send { player ->
        // After an error or a stop the player is idle and must be prepared again before it can play.
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        // At the end of the queue play alone does nothing: the item must start again.
        if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
        player.play()
    }

    override fun pause() = send(Player::pause)

    override fun seekTo(positionMs: Long) = send { player -> player.seekTo(positionMs) }

    override fun next() = send(Player::seekToNext)

    override fun previous() = send(Player::seekToPrevious)

    // Media3 keeps the pitch when only the speed is set.
    override fun setSpeed(speed: Float) = send { player ->
        // A value that is no number has no place between the limits, and the player would refuse it.
        if (!speed.isNaN()) {
            player.setPlaybackSpeed(speed.coerceIn(PlaybackController.MIN_SPEED, PlaybackController.MAX_SPEED))
        }
    }

    private fun send(command: (Player) -> Unit) {
        scope.launch { link.send(command) }
    }

    /** Starts to show [player], or the empty state when the player is gone. */
    private fun follow(player: Player?) {
        shown?.removeListener(playerListener)
        shown = player
        player?.addListener(playerListener)
        current.value = player?.toPlaybackState() ?: PlaybackState()
    }
}

/**
 * The player sends no event while the position moves, so the position is read on a timer.
 * The timer runs only while somebody collects the state: with no screen there is nothing to wake up for.
 */
private suspend fun MutableStateFlow<PlaybackState>.followPositionWhileWatched(shown: () -> Player?) {
    subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { watched ->
        while (watched) {
            val player = shown()
            if (player != null && value.isPlaying) {
                value = value.copy(positionMs = player.currentPosition.coerceAtLeast(0))
            }
            delay(POSITION_STEP)
        }
    }
}
