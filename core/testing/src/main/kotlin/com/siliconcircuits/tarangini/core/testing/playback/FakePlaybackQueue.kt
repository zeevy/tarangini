/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.playback

import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue

/** A playback queue for tests. A test reads [commands] to see what a screen asked for. It changes no state. */
class FakePlaybackQueue : PlaybackQueue {
    /** Every command received, oldest first, as short text such as `clear` or `move 3 to 0`. */
    val commands = mutableListOf<String>()

    override fun playNext(mediaUris: List<String>) {
        commands += "playNext $mediaUris"
    }

    override fun addToEnd(mediaUris: List<String>) {
        commands += "addToEnd $mediaUris"
    }

    override fun move(from: Int, to: Int) {
        commands += "move $from to $to"
    }

    override fun remove(index: Int) {
        commands += "remove $index"
    }

    override fun playAt(index: Int) {
        commands += "playAt $index"
    }

    override fun clear() {
        commands += "clear"
    }

    override fun keepOnlyCurrent() {
        commands += "keepOnlyCurrent"
    }

    override fun setShuffle(on: Boolean) {
        commands += "setShuffle $on"
    }

    override fun setRepeat(mode: RepeatMode) {
        commands += "setRepeat $mode"
    }
}
