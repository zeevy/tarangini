/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.domain.repository.StationStreams
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Moves a station to its next address when the one that plays fails.
 *
 * A station can be stored with more than one stream address. When the player gives up on one, the
 * next address of the same station is tried, and so on until each one has failed once. Then the
 * error stays, and a tap on "Try again" starts a new round.
 *
 * The item keeps its id and its texts. Only the address changes, so the queue, the saved place and
 * what the screens show stay the same.
 *
 * A stream that fell behind its live window is started again at the live edge. That is what the
 * person expects of radio after a long pause in the network.
 */
internal class StreamFailover(
    private val player: Player,
    private val streams: StationStreams,
    private val scope: CoroutineScope,
) : Player.Listener {
    private var failed = emptySet<String>()

    override fun onPlayerError(error: PlaybackException) {
        val item = player.currentMediaItem
        val address = item?.localConfiguration?.uri?.takeIf { it.isStreamAddress() }
        when {
            item == null || address == null -> Unit

            error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                player.seekToDefaultPosition()
                player.prepare()
            }

            else -> tryNextAddress(item, address)
        }
    }

    private fun tryNextAddress(item: MediaItem, address: Uri) {
        failed += address.toString()
        scope.launch {
            val next = addressesOf(address).firstOrNull { it.toString() !in failed }
            // The read took time. Another item may play by now, or the person may have tried again.
            // The item itself may be a new one with the same id: its texts can change at any time.
            val current = player.currentMediaItem?.takeIf { it.mediaId == item.mediaId && player.playerError != null }
            when {
                current == null -> Unit

                next == null -> failed = emptySet()

                else -> {
                    player.replaceMediaItem(player.currentMediaItemIndex, current.buildUpon().setUri(next).build())
                    player.prepare()
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) failed = emptySet()
    }

    /** The addresses of the station, without any this app would refuse to open. */
    private suspend fun addressesOf(address: Uri): List<Uri> = runCatching { streams.ofStationWith(address.toString()) }
        .getOrElse { failure -> if (failure is CancellationException) throw failure else emptyList() }
        .map(Uri::parse)
        .filter { it.isStreamAddress() }
}
