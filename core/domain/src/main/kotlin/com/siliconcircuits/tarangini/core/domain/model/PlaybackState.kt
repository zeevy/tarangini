/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * What the player is doing right now. The default value is "nothing loaded".
 *
 * @property queue the items loaded in the player, in the order they were loaded. With shuffle on, the player
 *   picks from them in another order, which is not shown here
 * @property currentIndex the place of the current item in [queue], or null when nothing is loaded
 * @property isPlaying true while sound is coming out
 * @property playWhenReady true when play was asked for, also while the player still loads. A play and pause
 *   button shows "pause" for it, so that a tap during loading pauses
 * @property isBuffering true while the player waits for data before it can go on
 * @property positionMs how far the current item has played
 * @property durationMs the length of the current item, or null when it is not known (live radio)
 * @property error why playback stopped, or null when it did not fail
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
    val error: PlaybackError? = null,
) {
    /** The item that plays or is paused, or null when nothing is loaded. */
    val currentItem: PlaybackItem?
        get() = currentIndex?.let(queue::getOrNull)
}
