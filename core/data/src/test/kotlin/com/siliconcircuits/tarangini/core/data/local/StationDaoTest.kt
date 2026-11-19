/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

import android.database.SQLException
import com.siliconcircuits.tarangini.core.data.local.entity.StationSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StationDaoTest {
    private val database = testDatabase()
    private val stations = database.stationDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `a station is saved with its streams in order`() = runTest {
        val id = database.addStation("Radio", uuid = "u1", urls = listOf("https://a", "https://b"))

        assertEquals("Radio", stations.station(id)?.name)
        assertEquals(id, stations.stationByRadioBrowserUuid("u1")?.id)
        assertEquals(listOf(0 to "https://a", 1 to "https://b"), stations.streams(id).map { it.position to it.url })
    }

    @Test
    fun `saving a station again changes it and replaces its streams`() = runTest {
        val id = database.addStation("Radio", urls = listOf("https://a", "https://b"))
        val stored = requireNotNull(stations.station(id))

        val again = stations.saveStation(stored.copy(name = "Radio 2"), stations.streams(id).drop(1))

        assertEquals(id, again)
        assertEquals("Radio 2", stations.station(id)?.name)
        assertEquals(listOf(0 to "https://b"), stations.streams(id).map { it.position to it.url })
    }

    @Test
    fun `one directory station is stored once, and typed in stations need no directory id`() = runTest {
        database.addStation("First", uuid = "same")
        database.addStation("Mine")
        database.addStation("Mine too")

        assertThrows(SQLException::class.java) { runBlocking { database.addStation("Second", uuid = "same") } }
        assertEquals(listOf("Mine", "Mine too"), stations.stationsBySource(StationSource.USER).map { it.name })
        assertEquals(listOf("First"), stations.stationsBySource(StationSource.RADIO_BROWSER).map { it.name })
    }

    @Test
    fun `removing a station removes its streams, its favourite mark and its play count`() = runTest {
        val id = database.addStation("Radio")
        database.favouriteDao().addStation(id)
        database.historyDao().recordStationPlay(id, at = 1)

        stations.deleteStation(id)

        assertNull(stations.station(id))
        assertTrue(stations.streams(id).isEmpty())
        assertTrue(database.favouriteDao().favouriteStations().isEmpty())
        assertTrue(database.historyDao().recentStations(10).isEmpty())
    }
}
