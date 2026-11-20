/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addStation
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.domain.model.Station
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultStationRepositoryTest {
    private val database = testDatabase()
    private var now = 1_000L
    private val stations = DefaultStationRepository(database.stationDao()) { now }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `a saved station is listed by name with its address, cleaned`() = runTest {
        stations.save(null, "  Zed   FM ", " https://zed.example/stream ")
        val id = stations.save(null, "Alpha", "http://alpha.example/a.pls")

        assertEquals(listOf("Alpha", "Zed FM"), stations.userStations().map { it.name })
        assertEquals(Station(id, "Alpha", listOf("http://alpha.example/a.pls")), stations.station(id))
        assertEquals(listOf("https://zed.example/stream"), stations.userStations().last().streamUrls)
    }

    @Test
    fun `a change keeps the id and the time it was made, and replaces name and address`() = runTest {
        val id = stations.save(null, "Old", "http://old.example/")
        now = 2_000

        assertEquals(id, stations.save(id, "New", "https://new.example/"))

        assertEquals(listOf(Station(id, "New", listOf("https://new.example/"))), stations.userStations())
        val stored = database.stationDao().station(id)
        assertEquals(1_000L, stored?.createdAt)
        assertEquals(2_000L, stored?.updatedAt)
    }

    @Test
    fun `a bad name or address is not saved`() = runTest {
        listOf(
            "" to "http://a.example/",
            "Name" to "file:///sdcard/a.mp3",
            "Name" to "http://a.example/ b",
        ).forEach { (name, address) ->
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { stations.save(null, name, address) }
            }
        }

        assertEquals(emptyList<Station>(), stations.userStations())
    }

    @Test
    fun `an address another station has is not saved twice, but a station keeps its own`() = runTest {
        val id = stations.save(null, "First", "http://same.example/")

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { stations.save(null, "Second", " http://same.example/ ") }
        }
        assertEquals(id, stations.save(id, "First again", "http://same.example/"))
        assertEquals(listOf("First again"), stations.userStations().map { it.name })
    }

    @Test
    fun `a station of the directory is not changed through this store`() = runTest {
        val directory = database.addStation(
            "Directory",
            uuid = "abc",
            urls = listOf("http://a.example/", "http://b.example/"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { stations.save(directory, "Mine", "http://mine.example/") }
        }
        assertEquals(2, database.stationDao().streams(directory).size)
    }

    @Test
    fun `a removed station is gone, and one that is not there removes nothing`() = runTest {
        val id = stations.save(null, "Radio", "http://radio.example/")
        val other = stations.save(null, "Other", "http://other.example/")

        stations.remove(id)
        stations.remove(999)

        assertNull(stations.station(id))
        assertEquals(listOf(other), stations.userStations().map { it.id })
    }

    @Test
    fun `stations from the directory are not listed with the ones the person added`() = runTest {
        database.addStation("Directory", uuid = "abc")
        stations.save(null, "Mine", "http://mine.example/")

        assertEquals(listOf("Mine"), stations.userStations().map { it.name })
    }

    @Test
    fun `a save and a remove are announced`() = runTest {
        val afterSave = async(start = CoroutineStart.UNDISPATCHED) { stations.changes().first() }
        val id = stations.save(null, "Radio", "http://radio.example/")
        afterSave.await()

        val afterRemove = async(start = CoroutineStart.UNDISPATCHED) { stations.changes().first() }
        stations.remove(id)
        afterRemove.await()
    }
}
