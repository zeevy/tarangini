/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.testing.playback

import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A playback controller for tests. A test sets [state] to what the player should report
 * and reads [commands] to see what a screen asked for.
 */
class FakePlaybackController : PlaybackController {
    override val state = MutableStateFlow(PlaybackState())

    /** Every command received, oldest first, as short text such as `pause` or `seekTo 5000`. */
    val commands = mutableListOf<String>()

    override fun play(mediaUris: List<String>, startIndex: Int) {
        commands += "play $mediaUris from $startIndex"
    }

    override fun resume() {
        commands += "resume"
    }

    override fun pause() {
        commands += "pause"
    }

    override fun seekTo(positionMs: Long) {
        commands += "seekTo $positionMs"
    }

    override fun next() {
        commands += "next"
    }

    override fun previous() {
        commands += "previous"
    }
}
