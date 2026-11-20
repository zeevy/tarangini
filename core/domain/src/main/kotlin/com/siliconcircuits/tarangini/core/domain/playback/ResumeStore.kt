/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.playback

import com.siliconcircuits.tarangini.core.domain.model.RepeatMode

/**
 * Keeps what was playing, so the app comes back with the same queue, item
 * and position after it was closed or killed.
 */
interface ResumeStore {
    /** Saves the whole queue and the place in it, together. */
    suspend fun saveQueue(playback: SavedPlayback)

    /** Saves only the place in the queue that is stored already. This is cheap and is called often. */
    suspend fun savePlace(place: ResumePlace)

    /**
     * What was saved, without the items whose file is gone, or null when
     * there is nothing to come back to.
     */
    suspend fun load(): SavedPlayback?
}

/**
 * The place in the queue where playback is.
 *
 * @property index the place of the current item in the queue, from 0
 * @property positionMs how far into the current item playback is
 */
data class ResumePlace(val index: Int, val positionMs: Long, val shuffle: Boolean, val repeat: RepeatMode)

/**
 * A queue and the place in it.
 *
 * @property mediaIds the id of every item of the queue, in order, as the player knows them
 */
data class SavedPlayback(val mediaIds: List<String>, val place: ResumePlace)
