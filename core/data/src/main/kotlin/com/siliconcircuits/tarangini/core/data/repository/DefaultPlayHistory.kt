/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.HistoryDao
import com.siliconcircuits.tarangini.core.data.mapper.toSong
import com.siliconcircuits.tarangini.core.data.mediastore.TimeSource
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.PlayHistory
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The play history from the database of the app. */
internal class DefaultPlayHistory @Inject constructor(private val history: HistoryDao, private val time: TimeSource) :
    PlayHistory {
    override suspend fun songPlayed(songId: Long) = history.recordSongPlay(songId, time.nowMs())

    override suspend fun recentSongs(limit: Int): List<Song> =
        history.recentSongs(limit.coerceIn(1, PlayHistory.MAX_RECENT)).map { it.toSong() }

    override fun changes(): Flow<Unit> = history.lastSongPlay().map { }

    override suspend fun clear() = history.clearSongPlays()
}
