/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * What the player is doing right now. The default value is "nothing loaded".
 *
 * @property queue the items loaded in the player, in the order they were loaded. With shuffle on, the player
 *   picks from them in another order: [playOrder]
 * @property currentIndex the place of the current item in [queue], or null when nothing is loaded
 * @property isPlaying true while sound is coming out
 * @property playWhenReady true when play was asked for, also while the player still loads. A play and pause
 *   button shows "pause" for it, so that a tap during loading pauses
 * @property isBuffering true while the player waits for data before it can go on
 * @property positionMs how far the current item has played
 * @property durationMs the length of the current item, or null when it is not known (live radio)
 * @property speed how fast the queue plays: 1 is the normal speed
 * @property isEnded true when the last item of the queue has played to its end
 * @property error why playback stopped, or null when it did not fail
 * @property format how the current item is stored, or null until the player has opened it
 * @property source the list the queue was started from, or null when it is not known or was changed by hand
 * @property playOrder with shuffle on, the places in [queue] from the current item to the last one the
 *   player will play, in the order it plays them. Null with shuffle off, when that is the order of [queue]
 */
data class PlaybackState(
    val queue: List<PlaybackItem> = emptyList(),
    val currentIndex: Int? = null,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long? = null,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val speed: Float = 1f,
    val isEnded: Boolean = false,
    val error: PlaybackError? = null,
    val format: SongFormat? = null,
    val playOrder: List<Int>? = null,
    val source: QueueSource? = null,
) {
    /** The item that plays or is paused, or null when nothing is loaded. */
    val currentItem: PlaybackItem?
        get() = currentIndex?.let(queue::getOrNull)
}
