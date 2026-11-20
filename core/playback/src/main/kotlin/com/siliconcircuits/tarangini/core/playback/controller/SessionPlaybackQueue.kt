/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.playback.di.PlaybackCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The queue commands of the phone UI. Each one is passed to the player of the media session,
 * which is the only owner of the queue.
 *
 * The scope it is given must run on the main thread: a media controller may be used only there.
 */
@Singleton
internal class SessionPlaybackQueue @Inject constructor(
    private val link: PlayerLink,
    private val loader: QueueLoader,
    @param:PlaybackCoroutineScope private val scope: CoroutineScope,
) : PlaybackQueue {
    override fun playNext(mediaUris: List<String>) = load(mediaUris) { player ->
        if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1
    }

    override fun addToEnd(mediaUris: List<String>) = load(mediaUris, Player::getMediaItemCount)

    override fun move(from: Int, to: Int) = send { player ->
        val places = 0 until player.mediaItemCount
        if (from in places && to in places && from != to) player.moveMediaItem(from, to)
    }

    override fun remove(index: Int) = send { player ->
        if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
    }

    override fun clear() {
        scope.launch {
            loader.cancel()
            link.send(Player::clearMediaItems)
        }
    }

    override fun setShuffle(on: Boolean) = send { player -> player.shuffleModeEnabled = on }

    override fun setRepeat(mode: RepeatMode) = send { player ->
        player.repeatMode = mode.toPlayerMode()
    }

    private fun load(mediaUris: List<String>, place: (Player) -> Int) {
        scope.launch { loader.add(mediaUris, place) }
    }

    private fun send(command: (Player) -> Unit) {
        scope.launch { link.send(command) }
    }
}
