/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction

/**
 * What a backup reads and writes that no other part needs.
 *
 * A song whose file is gone for now (a card that is out, a hidden folder) is part of every list
 * here. It is still a favourite and still in its playlist, and a backup must not lose it.
 */
@Dao
internal abstract class BackupDao {
    /** The files of the favourite songs, the one marked first at the start. */
    @Query(
        """
            SELECT s.id AS song_id, COALESCE(f.path, '') AS folder_path, s.file_name AS file_name
            FROM favourite_songs fs
            JOIN songs s ON s.id = fs.song_id
            LEFT JOIN folders f ON f.id = s.folder_id
            ORDER BY fs.added_at, fs.song_id
        """,
    )
    abstract suspend fun favouriteFiles(): List<SongFileRow>

    /** The files of the songs of a playlist in its order. */
    @Query(
        """
            SELECT s.id AS song_id, COALESCE(f.path, '') AS folder_path, s.file_name AS file_name
            FROM playlist_items i
            JOIN songs s ON s.id = i.song_id
            LEFT JOIN folders f ON f.id = s.folder_id
            WHERE i.playlist_id = :playlistId
            ORDER BY i.position, i.id
        """,
    )
    abstract suspend fun playlistFiles(playlistId: Long): List<SongFileRow>

    /** The files of the library with one of these names, whatever the case of the letters. At most 500 names. */
    @Query(
        """
            SELECT s.id AS song_id, COALESCE(f.path, '') AS folder_path, s.file_name AS file_name
            FROM songs s
            LEFT JOIN folders f ON f.id = s.folder_id
            WHERE s.file_name COLLATE NOCASE IN (:names)
        """,
    )
    abstract suspend fun filesNamed(names: List<String>): List<SongFileRow>

    /** The ids of the favourite songs. */
    @Query("SELECT song_id FROM favourite_songs")
    abstract suspend fun favouriteIds(): List<Long>

    /** True when the library has a song. */
    @Query("SELECT EXISTS (SELECT 1 FROM songs)")
    abstract suspend fun hasSongs(): Boolean

    /**
     * Marks the songs as favourites in one step, in the order given: each gets a later time than
     * the one before, so the last one is the newest. A song that is a favourite already keeps its time.
     */
    @Transaction
    open suspend fun addFavourites(songIds: List<Long>, firstAt: Long) {
        songIds.forEachIndexed { index, songId -> addFavourite(songId, firstAt + index) }
    }

    @Query("INSERT OR IGNORE INTO favourite_songs(song_id, added_at) SELECT id, :addedAt FROM songs WHERE id = :songId")
    protected abstract suspend fun addFavourite(songId: Long, addedAt: Long)

    /** Takes the mark off the songs in one step. */
    @Transaction
    open suspend fun removeFavourites(songIds: List<Long>) {
        songIds.forEach { removeFavourite(it) }
    }

    @Query("DELETE FROM favourite_songs WHERE song_id = :songId")
    protected abstract suspend fun removeFavourite(songId: Long)
}
