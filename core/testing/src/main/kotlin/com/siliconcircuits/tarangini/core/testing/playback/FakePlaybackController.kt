/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
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
