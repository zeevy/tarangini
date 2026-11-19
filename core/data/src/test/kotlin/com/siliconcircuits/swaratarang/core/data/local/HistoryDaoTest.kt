/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryDaoTest {
    private val database = testDatabase()
    private val history = database.historyDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `recent songs come newest first with no repeats`() = runTest {
        val a = database.addSong("a")
        val b = database.addSong("b")
        history.recordSongPlay(a, at = 1)
        history.recordSongPlay(b, at = 2)

        history.recordSongPlay(a, at = 3)

        assertEquals(listOf(a, b), history.recentSongs(10).map { it.song.id })
        assertEquals(listOf(a), history.recentSongs(1).map { it.song.id })
    }

    @Test
    fun `most played songs come by play count`() = runTest {
        val a = database.addSong("a")
        val b = database.addSong("b")
        history.recordSongPlay(b, at = 9)
        repeat(2) { history.recordSongPlay(a, at = 1) }

        assertEquals(listOf(a, b), history.mostPlayedSongs(10).map { it.song.id })
    }

    @Test
    fun `recent stations come newest first with no repeats`() = runTest {
        val a = database.addStation("A")
        val b = database.addStation("B")
        history.recordStationPlay(a, at = 1)
        history.recordStationPlay(b, at = 2)
        history.recordStationPlay(a, at = 3)

        assertEquals(listOf(a, b), history.recentStations(10).map { it.id })
        assertEquals(listOf(a), history.recentStations(1).map { it.id })
    }

    @Test
    fun `clearing the history keeps the songs and the stations`() = runTest {
        val song = database.addSong("a")
        val station = database.addStation("A")
        history.recordSongPlay(song, at = 1)
        history.recordStationPlay(station, at = 1)

        history.clearSongPlays()
        history.clearStationPlays()

        assertTrue(history.recentSongs(10).isEmpty())
        assertTrue(history.mostPlayedSongs(10).isEmpty())
        assertTrue(history.recentStations(10).isEmpty())
        assertEquals("A", database.stationDao().station(station)?.name)
        assertEquals(1, database.libraryDao().songsByTitle(10, 0).size)
    }
}
