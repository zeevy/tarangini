/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.testing.playback

import com.siliconcircuits.swaratarang.core.domain.model.RepeatMode
import com.siliconcircuits.swaratarang.core.domain.playback.PlaybackQueue

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

    override fun clear() {
        commands += "clear"
    }

    override fun setShuffle(on: Boolean) {
        commands += "setShuffle $on"
    }

    override fun setRepeat(mode: RepeatMode) {
        commands += "setRepeat $mode"
    }
}
