/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

/**
 * Goes on with the next item when the file of the current one is gone, for
 * example a song that was deleted after it was put in the queue. Any other
 * error stops the player as before.
 *
 * It stops after one round of the queue, so a queue of files that are all
 * gone does not go round for ever with repeat on.
 */
internal class SkipMissingFile(private val player: Player) : Player.Listener {
    private var skipped = 0

    override fun onPlayerError(error: PlaybackException) {
        val fileIsGone = error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        if (fileIsGone && player.hasNextMediaItem() && skipped < player.mediaItemCount) {
            skipped++
            player.seekToNextMediaItem()
            player.prepare()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) skipped = 0
    }
}
