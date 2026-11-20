/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query

/**
 * The first letters of the lists by name. Each query counts the rows the
 * list of [LibraryDao] shows and no others: a song whose file is gone, and
 * an album or an artist with no song that can be played, are left out.
 */
@Dao
internal interface LetterDao {
    /**
     * The first letters of the album titles in the order of the albums by title, with the same albums counted.
     * The first letter is read from the sort key, so the groups come in the order of the list.
     */
    @Query(
        """
            SELECT substr(al.title_key, 1, 1) AS letter, COUNT(*) AS total
            FROM albums al
            WHERE EXISTS (SELECT 1 FROM songs s WHERE s.album_id = al.id AND s.missing_since IS NULL)
            GROUP BY letter
            ORDER BY letter
        """,
    )
    suspend fun ofAlbums(): List<LetterRow>

    /** The first letters of the artist names in the order of the artists by name, with the same artists counted. */
    @Query(
        """
            SELECT substr(ar.name_key, 1, 1) AS letter, COUNT(*) AS total
            FROM artists ar
            WHERE EXISTS (SELECT 1 FROM songs s WHERE s.artist_id = ar.id AND s.missing_since IS NULL)
            GROUP BY letter
            ORDER BY letter
        """,
    )
    suspend fun ofArtists(): List<LetterRow>

    /** The first letters of the song titles in the order of the songs by title, with the same songs counted. */
    @Query(
        """
            SELECT substr(s.title_key, 1, 1) AS letter, COUNT(*) AS total
            FROM songs s
            WHERE s.missing_since IS NULL
            GROUP BY letter
            ORDER BY letter
        """,
    )
    suspend fun ofSongs(): List<LetterRow>
}
