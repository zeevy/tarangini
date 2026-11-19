/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private val STREAM_SCHEMES = setOf("http", "https")

/**
 * Keeps the phone awake in the way the current item needs, with the screen off.
 * A file needs only the processor. A stream also needs Wi-Fi to stay on.
 */
internal class WakeModeSwitch(private val player: ExoPlayer) : Player.Listener {
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        player.setWakeMode(wakeModeFor(mediaItem))
    }
}

/** The wake mode for [item]: network for a stream, local for a file and for nothing at all. */
@C.WakeMode
internal fun wakeModeFor(item: MediaItem?): Int =
    if (item?.localConfiguration?.uri?.scheme?.lowercase() in STREAM_SCHEMES) C.WAKE_MODE_NETWORK else C.WAKE_MODE_LOCAL
