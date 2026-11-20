/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addStation
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultStationStreamsTest {
    private val database = testDatabase()
    private val streams = DefaultStationStreams(database.stationStreamDao())

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `any address of a station gives all its addresses in their order`() = runTest {
        database.addStation("Other", urls = listOf("https://other.example/a"))
        database.addStation(
            "Radio",
            urls = listOf("https://radio.example/2", "https://radio.example/1", "http://b.example/"),
        )

        val expected = listOf("https://radio.example/2", "https://radio.example/1", "http://b.example/")
        assertEquals(expected, streams.ofStationWith("https://radio.example/1"))
        assertEquals(expected, streams.ofStationWith("http://b.example/"))
    }

    @Test
    fun `an address of no station gives nothing`() = runTest {
        database.addStation("Radio", urls = listOf("https://radio.example/1"))

        assertEquals(emptyList<String>(), streams.ofStationWith("https://radio.example/"))
        assertEquals(emptyList<String>(), streams.ofStationWith(""))
    }

    @Test
    fun `names are found by address, and the station stored first wins a shared address`() = runTest {
        database.addStation("First", urls = listOf("https://same.example/", "https://first.example/"))
        database.addStation("Second", urls = listOf("https://second.example/", "https://same.example/"))

        val names = streams.namesByAddress(
            listOf(
                "https://second.example/",
                "https://same.example/",
                "https://same.example/",
                "https://unknown.example/",
            ),
        )

        assertEquals(mapOf("https://second.example/" to "Second", "https://same.example/" to "First"), names)
        assertEquals(emptyMap<String, String>(), streams.namesByAddress(emptyList()))
    }

    @Test
    fun `an address that two stations have gives the station stored first`() = runTest {
        database.addStation("First", urls = listOf("https://same.example/", "https://first.example/"))
        database.addStation("Second", urls = listOf("https://second.example/", "https://same.example/"))

        assertEquals(
            listOf("https://same.example/", "https://first.example/"),
            streams.ofStationWith("https://same.example/"),
        )
    }
}
