/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * Puts the speed back to normal when a new queue starts. A speed is picked for one long
 * recording; the next thing a person plays is most often music, which must not start fast or slow.
 * A change of the queue that keeps the item that plays leaves the speed alone.
 */
internal class ResetSpeed(private val player: Player) : Player.Listener {
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) player.setPlaybackSpeed(1f)
    }
}
