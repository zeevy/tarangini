/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.domain.playback

import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
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
     */
    fun play(mediaUris: List<String>, startIndex: Int = 0)

    /** Goes on with the current item after a pause or an error. */
    fun resume()

    fun pause()

    /** Jumps to a position in the current item. */
    fun seekTo(positionMs: Long)

    /** Goes to the next item of the queue. Does nothing on the last item when repeat is off. */
    fun next()

    /** Goes to the start of the current item, or to the item before it in the first 3 seconds of the current one. */
    fun previous()
}
