/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.playback

import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import kotlinx.coroutines.flow.StateFlow

/**
 * The one place where a screen reads what the player does and tells it what to do.
 *
 * A command may be sent while a screen of the app is open. When the player is not reachable yet,
 * the command runs as soon as it is. A command sent with no screen open is ignored.
 */
interface PlaybackController {
    /** What the player is doing. It is "nothing loaded" while the player is not reachable. */
    val state: StateFlow<PlaybackState>

    /**
     * Replaces the queue and starts to play.
     *
     * @param mediaUris the addresses of the audio files, as MediaStore gives them
     * @param startIndex the place in [mediaUris] of the item to play first
     * @param source the list the queue comes from, or null when it has no name worth showing
     */
    fun play(mediaUris: List<String>, startIndex: Int = 0, source: QueueSource? = null)

    /**
     * Plays what a spoken request asks for: the song, artist or album of the library that fits [words] best.
     * With no words it goes on with what was playing last, or plays songs of the library in a new order.
     */
    fun playSpoken(words: String)

    /** Goes on with the current item after a pause or an error. At the end of the queue the item starts again. */
    fun resume()

    fun pause()

    /** Jumps to a position in the current item. */
    fun seekTo(positionMs: Long)

    /** Goes to the next item of the queue. Does nothing on the last item when repeat is off. */
    fun next()

    /** Goes to the start of the current item, or to the item before it in the first 3 seconds of the current one. */
    fun previous()

    /**
     * Sets how fast the queue plays, with the pitch kept. The value is held between [MIN_SPEED]
     * and [MAX_SPEED]. A new queue starts at normal speed again.
     */
    fun setSpeed(speed: Float)

    companion object {
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2f
    }
}
