/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.resume

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback

/** Puts a saved queue into the player at its place. It does not prepare and does not play. */
internal fun Player.fill(playback: SavedPlayback) {
    // One call: the list comes from the database of this app, in the same process, and nothing waits to sound.
    setMediaItems(playback.mediaIds.map(::toMediaItem), playback.place.index, playback.place.positionMs)
    shuffleModeEnabled = playback.place.shuffle
    repeatMode = when (playback.place.repeat) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    }
}

/** Where the player is now, in the form that is saved. */
internal fun Player.place() = ResumePlace(
    index = currentMediaItemIndex,
    positionMs = currentPosition,
    shuffle = shuffleModeEnabled,
    repeat = when (repeatMode) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    },
)

/** True for what moves the place: a pause or a start, another item, a seek, a new queue, shuffle and repeat. */
internal fun Player.Events.movesThePlace(): Boolean = containsAny(
    Player.EVENT_IS_PLAYING_CHANGED,
    Player.EVENT_MEDIA_ITEM_TRANSITION,
    Player.EVENT_POSITION_DISCONTINUITY,
    Player.EVENT_TIMELINE_CHANGED,
    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
    Player.EVENT_REPEAT_MODE_CHANGED,
)

/** The saved queue in the form the session takes when a caller asks to go on. */
// The class is API that Media3 may still change.
@OptIn(UnstableApi::class)
internal fun SavedPlayback.toResumption() =
    MediaItemsWithStartPosition(mediaIds.map(::toMediaItem), place.index, place.positionMs)

private fun toMediaItem(mediaId: String): MediaItem = MediaItem.Builder().setMediaId(mediaId).setUri(mediaId).build()
