/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
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
