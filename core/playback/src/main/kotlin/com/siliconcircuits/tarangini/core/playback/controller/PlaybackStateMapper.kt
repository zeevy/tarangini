/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode

private const val FIRST_PARSING_ERROR = 3000
private const val LAST_DECODING_ERROR = 4999

/**
 * Reads everything a screen needs from the player, at this moment.
 *
 * @param knownQueue the queue read last time, when the caller knows it has not changed. It saves reading
 *   every item of a long queue again
 */
internal fun Player.toPlaybackState(knownQueue: List<PlaybackItem>? = null): PlaybackState {
    val current = currentMediaItemIndex.takeIf { mediaItemCount > 0 }
    val queue = knownQueue ?: List(mediaItemCount) { index ->
        val item = getMediaItemAt(index)
        // The player knows the tags of the current item only: it has read that file.
        item.mediaMetadata.toPlaybackItem(item.mediaId, tags = mediaMetadata.takeIf { index == current })
    }
    return PlaybackState(
        queue = queue,
        currentIndex = current,
        isPlaying = isPlaying,
        playWhenReady = playWhenReady,
        isBuffering = playbackState == Player.STATE_BUFFERING,
        positionMs = currentPosition.coerceAtLeast(0),
        durationMs = duration.takeIf { it != C.TIME_UNSET && current != null },
        shuffle = shuffleModeEnabled,
        repeat = repeatMode.toRepeatMode(),
        error = playerError?.toPlaybackError(),
    )
}

private fun MediaMetadata.toPlaybackItem(id: String, tags: MediaMetadata?): PlaybackItem = PlaybackItem(
    id = id,
    title = (tags?.title ?: title)?.toString(),
    artist = (tags?.artist ?: artist)?.toString(),
    album = (tags?.albumTitle ?: albumTitle)?.toString(),
    artworkUri = (tags?.artworkUri ?: artworkUri)?.toString(),
)

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

private fun PlaybackException.toPlaybackError(): PlaybackError = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
    -> PlaybackError.FILE_NOT_AVAILABLE

    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    -> PlaybackError.NETWORK

    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
    in FIRST_PARSING_ERROR..LAST_DECODING_ERROR,
    -> PlaybackError.CANNOT_PLAY

    else -> PlaybackError.UNKNOWN
}
