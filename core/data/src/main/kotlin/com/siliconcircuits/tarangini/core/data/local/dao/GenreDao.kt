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

/** The genres of the songs. A song has one genre or more; a song with no genre tag has the genre with no name. */
@Dao
internal abstract class GenreDao {
    /**
     * Saves a genre when it is new and returns its id. Two names with the same sort form are one
     * genre, so "Rock" and "rock" do not show as two.
     */
    @Transaction
    open suspend fun saveGenre(name: String, nameKey: String): Long {
        insertGenre(name, nameKey)
        return genreId(nameKey)
    }

    @Query("INSERT OR IGNORE INTO genres(name, name_key) VALUES(:name, :nameKey)")
    protected abstract suspend fun insertGenre(name: String, nameKey: String)

    @Query("SELECT id FROM genres WHERE name_key = :nameKey")
    protected abstract suspend fun genreId(nameKey: String): Long

    /** Makes [genreIds] the genres of a song, in place of the ones it had. */
    @Transaction
    open suspend fun setGenres(songId: Long, genreIds: List<Long>) {
        clearGenres(songId)
        genreIds.forEach { addGenre(songId, it) }
    }

    @Query("DELETE FROM song_genres WHERE song_id = :songId")
    protected abstract suspend fun clearGenres(songId: Long)

    @Query("INSERT OR IGNORE INTO song_genres(song_id, genre_id) VALUES(:songId, :genreId)")
    protected abstract suspend fun addGenre(songId: Long, genreId: Long)

    /** Removes the genres no song has any more. */
    @Query("DELETE FROM genres WHERE id NOT IN (SELECT genre_id FROM song_genres)")
    abstract suspend fun deleteUnusedGenres()

    /**
     * The genres that have a song to play, by name, with the genre with no name last. The count
     * leaves out a song whose file is gone.
     */
    @Query(
        """
            SELECT g.id AS id, g.name AS name, g.name_key AS name_key, COUNT(s.id) AS song_count
            FROM genres g
            JOIN song_genres sg ON sg.genre_id = g.id
            JOIN songs s ON s.id = sg.song_id AND s.missing_since IS NULL
            GROUP BY g.id
            ORDER BY g.name_key = '', g.name_key, g.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun genres(limit: Int, offset: Int): List<GenreRow>

    /** One genre with its count, or null when it is gone or has no song to play. */
    @Query(
        """
            SELECT g.id AS id, g.name AS name, g.name_key AS name_key, COUNT(s.id) AS song_count
            FROM genres g
            JOIN song_genres sg ON sg.genre_id = g.id
            JOIN songs s ON s.id = sg.song_id AND s.missing_since IS NULL
            WHERE g.id = :genreId
            GROUP BY g.id
        """,
    )
    abstract suspend fun genre(genreId: Long): GenreRow?

    /** The songs of a genre by title. */
    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM song_genres sg
            JOIN songs s ON s.id = sg.song_id
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE sg.genre_id = :genreId AND s.missing_since IS NULL
            ORDER BY s.title_key, s.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun songsOfGenre(genreId: Long, limit: Int, offset: Int): List<SongRow>
}

/** A genre with the number of its songs that can be played. */
internal data class GenreRow(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
    @ColumnInfo(name = "song_count") val songCount: Int,
)

/** One genre of one song, by the key of the genre. */
internal data class SongGenreKey(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "name_key") val nameKey: String,
)
