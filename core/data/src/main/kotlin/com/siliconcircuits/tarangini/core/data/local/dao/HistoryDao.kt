/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.siliconcircuits.tarangini.core.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes play counts and last played times of songs and stations. */
@Dao
internal interface HistoryDao {
    /**
     * Counts one play of a song the library knows. One statement, so two plays at once are both
     * counted and a song that was removed in the meantime is left alone.
     */
    @Query(
        """
            INSERT INTO song_plays(song_id, play_count, last_played_at)
            SELECT id, 1, :at FROM songs WHERE id = :songId
            ON CONFLICT(song_id) DO UPDATE SET play_count = play_count + 1, last_played_at = excluded.last_played_at
        """,
    )
    suspend fun recordSongPlay(songId: Long, at: Long)

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM song_plays p
            JOIN songs s ON s.id = p.song_id
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY p.last_played_at DESC, p.song_id DESC
            LIMIT :limit
        """,
    )
    suspend fun recentSongs(limit: Int): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM song_plays p
            JOIN songs s ON s.id = p.song_id
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY p.play_count DESC, p.last_played_at DESC, p.song_id DESC
            LIMIT :limit
        """,
    )
    suspend fun mostPlayedSongs(limit: Int): List<SongRow>

    /** The time of the last play. A new value comes after every change of the history. */
    @Query("SELECT MAX(last_played_at) FROM song_plays")
    fun lastSongPlay(): Flow<Long?>

    @Query("DELETE FROM song_plays")
    suspend fun clearSongPlays()

    @Query(
        """
            INSERT INTO station_plays(station_id, play_count, last_played_at) VALUES(:stationId, 1, :at)
            ON CONFLICT(station_id) DO UPDATE SET play_count = play_count + 1, last_played_at = excluded.last_played_at
        """,
    )
    suspend fun recordStationPlay(stationId: Long, at: Long)

    @Query(
        """
            SELECT st.* FROM station_plays p
            JOIN stations st ON st.id = p.station_id
            ORDER BY p.last_played_at DESC, p.station_id DESC
            LIMIT :limit
        """,
    )
    suspend fun recentStations(limit: Int): List<StationEntity>

    @Query("DELETE FROM station_plays")
    suspend fun clearStationPlays()
}
