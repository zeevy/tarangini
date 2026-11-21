/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 2 to 3: the tables for genres.
 *
 * Both tables start empty. The next scan sees that a song has no genre yet and reads its tags
 * again, so nothing is filled here and nothing that exists is changed.
 */
internal object GenreTables : Migration(FROM_VERSION, TO_VERSION) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `genres` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `name_key` TEXT NOT NULL)",
        )
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_genres_name_key` ON `genres` (`name_key`)")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `song_genres` (" +
                "`song_id` INTEGER NOT NULL, `genre_id` INTEGER NOT NULL, PRIMARY KEY(`song_id`, `genre_id`), " +
                "FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`genre_id`) REFERENCES `genres`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_song_genres_genre_id` ON `song_genres` (`genre_id`)")
    }
}

private const val FROM_VERSION = 2
private const val TO_VERSION = 3
