/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.sleep

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.playback.SleepTimer
import com.siliconcircuits.tarangini.core.playback.di.PlaybackCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The sleep timer of the app. There is one, and the playback service hands it the player while
 * the service lives. The screens and the service are in one process, so a screen sets the timer
 * here and the timer acts on the player directly.
 *
 * Everything here runs on the main thread.
 */
// "Pause at the end of the item" is API of Media3 that may still change.
@OptIn(UnstableApi::class)
@Singleton
internal class DefaultSleepTimer @Inject constructor(@param:PlaybackCoroutineScope private val scope: CoroutineScope) :
    SleepTimer,
    Player.Listener {
    private val current = MutableStateFlow<SleepState>(SleepState.Off)
    override val state: StateFlow<SleepState> = current.asStateFlow()

    private var player: ExoPlayer? = null
    private var countdown: Job? = null

    /** The service has made its player. */
    fun attach(player: ExoPlayer) {
        this.player = player
        player.addListener(this)
        // "At the end of the song" may have been picked before the service was there.
        player.pauseAtEndOfMediaItems = current.value == SleepState.EndOfTrack
    }

    /** The service ends. A timer that was set ends with it: there is nothing left to stop. */
    fun detach() {
        cancel()
        player?.removeListener(this)
        player = null
    }

    override fun stopAfter(minutes: Int) {
        cancel()
        countdown = scope.launch {
            var leftMs = minutes.coerceIn(1, SleepTimer.MAX_MINUTES) * MS_PER_MINUTE
            while (leftMs > 0) {
                current.value = SleepState.Timed(leftMs)
                // The last seconds get quieter step by step down to silence, so the sound does not stop at once.
                if (leftMs <= FADE_MS) player?.volume = (leftMs - TICK_MS).toFloat() / FADE_MS
                delay(TICK_MS)
                leftMs -= TICK_MS
            }
            player?.pause()
            player?.volume = 1f
            current.value = SleepState.Off
        }
    }

    override fun stopAtEndOfTrack() {
        cancel()
        // The player itself pauses at the end of the item, so nothing of the next one is heard.
        player?.pauseAtEndOfMediaItems = true
        current.value = SleepState.EndOfTrack
    }

    override fun cancel() {
        countdown?.cancel()
        countdown = null
        player?.volume = 1f
        player?.pauseAtEndOfMediaItems = false
        current.value = SleepState.Off
    }

    /** The player has paused at the end of the item, as asked. The timer has done its work. */
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) endOfTrackReached()
    }

    /** The last item of the queue has ended. There is nothing left to stop. */
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) endOfTrackReached()
    }

    private fun endOfTrackReached() {
        if (current.value == SleepState.EndOfTrack) cancel()
    }

    private companion object {
        const val MS_PER_MINUTE = 60_000L
        const val TICK_MS = 1_000L
        const val FADE_MS = 5_000L
    }
}
