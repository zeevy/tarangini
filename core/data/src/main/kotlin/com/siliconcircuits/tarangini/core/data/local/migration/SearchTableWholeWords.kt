/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.siliconcircuits.tarangini.core.data.local.SEARCH_TOKEN_CHARS

/**
 * Version 1 to 2: the search table keeps a word of an Indian script whole.
 *
 * The way a search table cuts text into words is fixed when the table is made, so the table is
 * made again and filled from the songs. It holds only a copy of the title, the artist and the
 * album of each song; nothing a person made is in it, and no other table is touched.
 */
internal object SearchTableWholeWords : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `song_search`")
        connection.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `song_search` USING FTS4(" +
                "`title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, " +
                "tokenize=unicode61 `$SEARCH_TOKEN_CHARS`)",
        )
        connection.execSQL(
            """
            INSERT INTO song_search(rowid, title, artist, album)
            SELECT s.id, s.title, ar.name, al.title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            """.trimIndent(),
        )
    }
}
