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
import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback
import com.siliconcircuits.tarangini.core.playback.controller.toPlayerMode
import com.siliconcircuits.tarangini.core.playback.controller.toRepeatMode
import com.siliconcircuits.tarangini.core.playback.mapper.playableItem

/** Puts a saved queue into the player at its place. It does not prepare and does not play. */
internal fun Player.fill(items: List<MediaItem>, place: ResumePlace) {
    // One call: the list comes from the database of this app, in the same process, and nothing waits to sound.
    setMediaItems(items, place.index, place.positionMs)
    shuffleModeEnabled = place.shuffle
    repeatMode = place.repeat.toPlayerMode()
}

/** The queue of the player and where it is now, in the form that is saved. */
internal fun Player.playback() = SavedPlayback(List(mediaItemCount) { getMediaItemAt(it).mediaId }, place())

/** Where the player is now, in the form that is saved. */
internal fun Player.place() = ResumePlace(
    index = currentMediaItemIndex,
    positionMs = currentPosition,
    shuffle = shuffleModeEnabled,
    repeat = repeatMode.toRepeatMode(),
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
