/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import com.siliconcircuits.tarangini.core.domain.station.StreamTest
import com.siliconcircuits.tarangini.core.domain.station.StreamTestFault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultStreamTesterTest {
    private val server = stationServer().apply { start() }
    private val tester = DefaultStreamTester({ OkHttpClient() }, Dispatchers.IO)

    @After
    fun stop() = server.close()

    private fun at(path: String) = server.url(path).toString()

    private fun failed(fault: StreamTestFault) = StreamTest.Failed(fault)

    @Test
    fun `a station of each kind is found, with what the server says about it`() = runTest {
        assertEquals(StreamTest.Found("audio/mpeg", "Test station"), tester.test(at("/icecast.mp3")))
        assertEquals(StreamTest.Found("audio/mpeg", "My Radio", 128), tester.test(at("/named.mp3")))
        assertEquals(StreamTest.Found("audio/mpeg"), tester.test(at("/shoutcast")))
        assertEquals(StreamTest.Found("audio/aac"), tester.test("  " + at("/radio.aac") + " "))
        assertEquals(StreamTest.Found("application/vnd.apple.mpegurl"), tester.test(at("/live.m3u8")))
        assertEquals(StreamTest.Found("audio"), tester.test(at("/no-type")))
    }

    @Test
    fun `a playlist file is followed to its stream`() = runTest {
        assertEquals(StreamTest.Found("audio/mpeg", "Test station"), tester.test(at("/station.pls")))
        assertEquals(StreamTest.Found("audio/aac"), tester.test(at("/station.m3u")))
    }

    @Test
    fun `a playlist file with a fault says so`() = runTest {
        assertEquals(failed(StreamTestFault.BAD_PLAYLIST), tester.test(at("/self.pls")))
        assertEquals(failed(StreamTestFault.BAD_PLAYLIST), tester.test(at("/empty.pls")))
        assertEquals(failed(StreamTestFault.BAD_PLAYLIST), tester.test(at("/files-only.m3u")))
    }

    @Test
    fun `an address the server does not have, a page and a stream the player would not know are not found`() = runTest {
        assertEquals(failed(StreamTestFault.BAD_ANSWER), tester.test(at("/nothing-here")))
        assertEquals(failed(StreamTestFault.NOT_A_STREAM), tester.test(at("/page.html")))
        assertEquals(failed(StreamTestFault.NOT_A_STREAM), tester.test(at("/hls-without-ending")))
    }

    @Test
    fun `an address the app does not open is never asked for`() = runTest {
        listOf(
            "file:///sdcard/a.mp3",
            "content://media/external/audio/media/1",
            "",
            "radio",
            "http://me:pw@radio.example/",
        )
            .forEach { assertEquals(it, failed(StreamTestFault.BAD_ADDRESS), tester.test(it)) }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a server that is not there gives no answer`() = runTest {
        val gone = at("/icecast.mp3")
        server.close()

        assertEquals(failed(StreamTestFault.NO_ANSWER), tester.test(gone))
    }
}
