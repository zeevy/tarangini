/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction

/** What the import and the export of a playlist file read and write. */
@Dao
internal abstract class PlaylistFileDao {
    /** Adds the songs at the end of a playlist in the order given. All of them, or none. */
    @Transaction
    open suspend fun addItems(playlistId: Long, songIds: List<Long>, at: Long) {
        songIds.forEach { addItem(playlistId, it, at) }
    }

    @Query(
        """
            INSERT INTO playlist_items(playlist_id, song_id, position, added_at)
            SELECT p.id, s.id,
            (SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_items WHERE playlist_id = p.id),
            :at
            FROM playlists p, songs s WHERE p.id = :playlistId AND s.id = :songId
        """,
    )
    protected abstract suspend fun addItem(playlistId: Long, songId: Long, at: Long): Long

    /**
     * The files of the library with one of these names, whatever the case of the letters.
     * A song whose file is gone is left out. At most 500 names per call.
     */
    @Query(
        """
            SELECT s.id AS song_id, COALESCE(f.path, '') AS folder_path, s.file_name AS file_name
            FROM songs s
            LEFT JOIN folders f ON f.id = s.folder_id
            WHERE s.missing_since IS NULL AND s.file_name COLLATE NOCASE IN (:names)
        """,
    )
    abstract suspend fun filesNamed(names: List<String>): List<SongFileRow>

    /** The songs of a playlist in its order, as a playlist file names them. A song whose file is gone is left out. */
    @Query(
        """
            SELECT COALESCE(f.path, '') AS folder_path, s.file_name AS file_name, s.title AS title,
                ar.name AS artist, s.duration_ms AS duration_ms
            FROM playlist_items i
            JOIN songs s ON s.id = i.song_id
            JOIN artists ar ON ar.id = s.artist_id
            LEFT JOIN folders f ON f.id = s.folder_id
            WHERE i.playlist_id = :playlistId AND s.missing_since IS NULL
            ORDER BY i.position, i.id
        """,
    )
    abstract suspend fun filesOf(playlistId: Long): List<PlaylistFileRow>
}

/** The file of a song: its folder as the library saves it, and its name. */
internal data class SongFileRow(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "file_name") val fileName: String,
)

/** One line of a playlist file. */
internal data class PlaylistFileRow(
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
)
