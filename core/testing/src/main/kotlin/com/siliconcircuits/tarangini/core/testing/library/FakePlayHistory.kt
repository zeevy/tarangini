/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.PlayHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A play history kept in memory. A test sets [known] to the songs of its library. */
class FakePlayHistory : PlayHistory {
    /** The songs that can be shown. */
    var known: List<Song> = emptyList()

    /** The id of every play that was counted, the first one at the start. */
    val played = MutableStateFlow<List<Long>>(emptyList())

    override suspend fun songPlayed(songId: Long) {
        played.value += songId
    }

    override suspend fun recentSongs(limit: Int): List<Song> =
        played.value.reversed().distinct().mapNotNull { id -> known.firstOrNull { it.id == id } }.take(limit)

    override suspend fun mostPlayedSongs(limit: Int): List<Song> {
        val plays = played.value
        // The most plays first; songs with the same count in the order they were heard last.
        return plays.distinct()
            .sortedWith(
                compareByDescending<Long> { id ->
                    plays.count { it == id }
                }.thenByDescending(plays::lastIndexOf),
            )
            .mapNotNull { id -> known.firstOrNull { it.id == id } }
            .take(limit)
    }

    override fun changes(): Flow<Unit> = played.map { }

    override suspend fun clear() {
        played.value = emptyList()
    }
}
