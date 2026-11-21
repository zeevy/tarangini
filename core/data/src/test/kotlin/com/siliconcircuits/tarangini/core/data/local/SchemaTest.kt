/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a database version from its committed schema file and brings it to
 * the newest version. A new version adds its migration to [migrations] and
 * one test here.
 */
@RunWith(RobolectricTestRunner::class)
class SchemaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val migrations = TaranginiDatabase.MIGRATIONS

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = instrumentation.targetContext.getDatabasePath("schema-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = TaranginiDatabase::class,
    )

    @Test
    fun `version 1 is built from its committed schema and matches the entities`() = runTest {
        helper.createDatabase(version = 1).close()

        helper.runMigrationsAndValidate(version = 2, migrations = migrations).close()
    }

    @Test
    fun `version 2 makes the search table again and keeps every song, favourite and playlist`() = runTest {
        helper.createDatabase(version = 1).use { old ->
            old.execSQL("INSERT INTO artists(id, name, name_key) VALUES (1, 'అన్నమయ్య', 'a')")
            old.execSQL("INSERT INTO albums(id, title, title_key, artist_id) VALUES (1, 'కీర్తనలు', 'k', 1)")
            listOf(1 to "గోవిందా", 2 to "గానం").forEach { (id, title) ->
                old.execSQL(
                    "INSERT INTO songs(id, source, source_id, title, title_key, artist_id, album_id, file_name, " +
                        "mime_type, duration_ms, size_bytes, date_added, date_modified) " +
                        "VALUES ($id, 'LOCAL', '$id', '$title', 't$id', 1, 1, '$id.mp3', 'audio/mpeg', 1000, 10, 1, 1)",
                )
                old.execSQL("INSERT INTO song_search(rowid, title, artist, album) VALUES ($id, '$title', 'a', 'b')")
            }
            old.execSQL("INSERT INTO favourite_songs(song_id, added_at) VALUES (1, 5)")
            old.execSQL("INSERT INTO playlists(id, name, created_at, updated_at) VALUES (1, 'Morning', 5, 5)")
            old.execSQL(
                "INSERT INTO playlist_items(id, playlist_id, song_id, position, added_at) VALUES (1, 1, 2, 0, 5)",
            )
        }

        helper.runMigrationsAndValidate(version = 2, migrations = migrations).use { now ->
            assertEquals(listOf(1L, 2L), now.longs("SELECT id FROM songs ORDER BY id"))
            assertEquals(listOf(1L), now.longs("SELECT song_id FROM favourite_songs"))
            assertEquals(listOf(2L), now.longs("SELECT song_id FROM playlist_items WHERE playlist_id = 1"))
            // The table is filled from the songs: the artist of the old row was "a".
            assertEquals(
                listOf(1L, 2L),
                now.longs("SELECT rowid FROM song_search WHERE song_search MATCH 'artist:అన్న*'"),
            )
            // A word is whole now: the start "గా" is not the start of "గోవిందా".
            assertEquals(listOf(2L), now.longs("SELECT rowid FROM song_search WHERE song_search MATCH '\"గా*\"'"))
        }
    }

    private fun SQLiteConnection.longs(sql: String): List<Long> = prepare(sql).use { rows ->
        buildList { while (rows.step()) add(rows.getLong(0)) }
    }
}
