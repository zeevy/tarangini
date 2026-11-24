/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.siliconcircuits.tarangini.core.domain.premium.CrossfadeLength
import com.siliconcircuits.tarangini.core.playback.stream.isStreamAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Works out on the main thread how long the crossfade into the next song is, and keeps it in
 * [fadeUs] for the playback thread. It follows the order the player really plays: the next item
 * with shuffle and repeat all, none at the end of the queue or with repeat one.
 */
internal class CrossfadePlan(private val length: CrossfadeLength) : Player.Listener {
    private var lengthMs = 0
    private var player: Player? = null
    private var job: Job? = null

    /** The length of the fade into the next song in microseconds, 0 for none. Read on any thread. */
    @Volatile
    var fadeUs = 0L
        private set

    /** Follows [player] and the set length, in [scope] on the main thread, until [end]. */
    fun follow(player: Player, scope: CoroutineScope) {
        this.player = player
        player.addListener(this)
        job = scope.launch {
            length.millis().collect { millis ->
                lengthMs = millis
                update()
            }
        }
    }

    /** Stops following. */
    fun end() {
        job?.cancel()
        job = null
        player?.removeListener(this)
        player = null
        fadeUs = 0
    }

    override fun onEvents(player: Player, events: Player.Events) = update()

    private fun update() {
        val player = player ?: return
        val timeline = player.currentTimeline
        fadeUs = fadeUsFor(
            lengthMs = lengthMs,
            current = timeline.fadeSongAt(player.currentMediaItemIndex),
            next = timeline.fadeSongAt(player.nextMediaItemIndex),
            repeatOne = player.repeatMode == Player.REPEAT_MODE_ONE,
            speed = player.playbackParameters.speed,
        )
    }
}

/** What the plan needs to know of a song. */
internal data class FadeSong(val durationUs: Long, val album: String?, val isStream: Boolean)

/**
 * The fade from [current] into [next] in microseconds, or 0 for none. There is no fade at the end
 * of the queue, with repeat one, at a speed other than 1x, for a station, or between two songs of
 * one album, which are often made to run into each other with no gap. A fade takes at most half
 * of each song, so a short song still has its own middle.
 */
internal fun fadeUsFor(lengthMs: Int, current: FadeSong?, next: FadeSong?, repeatOne: Boolean, speed: Float): Long {
    if (current == null || next == null) return 0
    val sameAlbum = current.album != null && current.album == next.album
    val fades = lengthMs > 0 && !current.isStream && !next.isStream && !repeatOne && speed == 1f && !sameAlbum
    val wanted = lengthMs.coerceIn(0, MAX_FADE_MS) * MICROS_PER_MS
    return if (fades) minOf(wanted, current.durationUs / 2, next.durationUs / 2) else 0
}

private fun Timeline.fadeSongAt(index: Int): FadeSong? {
    if (index == C.INDEX_UNSET || index >= windowCount) return null
    val window = getWindow(index, Timeline.Window())
    val item = window.mediaItem
    val metadata = item.mediaMetadata
    val uri = item.localConfiguration?.uri
    val album = metadata.albumTitle?.let { title -> "$title\u0000${metadata.albumArtist ?: metadata.artist ?: ""}" }
    val durationMs = window.durationMs
    val stream = window.isLive || durationMs == C.TIME_UNSET || uri?.isStreamAddress() == true
    return FadeSong(durationMs * MICROS_PER_MS, album, stream)
}

private const val MAX_FADE_MS = 12_000
private const val MICROS_PER_MS = 1_000L
