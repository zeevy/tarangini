/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Song
import kotlinx.coroutines.flow.Flow

/** What the person heard: one entry per song, with the time it played last and how often it played. */
interface PlayHistory {
    /** Counts one play of a song, now. A song the library does not know is left alone. */
    suspend fun songPlayed(songId: Long)

    /** The songs heard last whose file is there, the newest first, each song once. At most [MAX_RECENT]. */
    suspend fun recentSongs(limit: Int = MAX_RECENT): List<Song>

    /**
     * The songs heard most often whose file is there, the one with the most plays first. Songs with
     * the same count come in the order they were heard last. At most [MAX_RECENT].
     */
    suspend fun mostPlayedSongs(limit: Int = MAX_RECENT): List<Song>

    /** Fires at the start and after every play that was counted, and after the history was cleared. */
    fun changes(): Flow<Unit>

    /** Forgets every play, the counts too. */
    suspend fun clear()

    companion object {
        /** A list of recent songs is short: it is for a quick way back, not a record. */
        const val MAX_RECENT = 50
    }
}
