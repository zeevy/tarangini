/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultPlayHistoryTest {
    private val database = testDatabase()
    private var now = 1_000L
    private val history = DefaultPlayHistory(database.historyDao()) { now++ }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `recent songs come newest first, each song once, and the plays are counted`() = runTest {
        val first = database.addSong("First", sourceId = "1")
        val second = database.addSong("Second", sourceId = "2")

        listOf(first, second, first).forEach { history.songPlayed(it) }

        assertEquals(listOf("First", "Second"), history.recentSongs().map { it.title })
        assertEquals(listOf("First"), history.recentSongs(limit = 1).map { it.title })
        assertEquals(listOf("First", "Second"), database.historyDao().mostPlayedSongs(10).map { it.song.title })
    }

    @Test
    fun `a song that is not there is not counted, and a clear forgets every play`() = runTest {
        val song = database.addSong("Song", sourceId = "3")
        history.songPlayed(song)
        history.songPlayed(song + 100)
        val before = history.recentSongs().map { it.title }

        history.clear()
        history.changes().first()

        assertEquals(listOf("Song"), before)
        assertEquals(emptyList<String>(), history.recentSongs().map { it.title })
    }
}
