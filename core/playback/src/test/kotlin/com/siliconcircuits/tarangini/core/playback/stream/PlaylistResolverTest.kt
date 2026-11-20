/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import java.io.IOException
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistResolverTest {
    private val server: MockWebServer = stationServer()
    private val resolver = PlaylistResolver { OkHttpClient() }

    @After
    fun stopServer() = server.close()

    private fun at(path: String): HttpUrl = server.url(path)

    private fun refused(path: String): String =
        assertThrows(PlaylistException::class.java) { resolver.resolve(at(path)) }.message.orEmpty()

    @Test
    fun `an address that is no playlist file comes back as it is, with no call`() {
        assertEquals(at("/icecast.mp3"), resolver.resolve(at("/icecast.mp3")))
        assertEquals(at("/live.m3u8"), resolver.resolve(at("/live.m3u8")))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a pls and an m3u file give their stream, also when the entry is relative`() {
        assertEquals(at("/icecast.mp3"), resolver.resolve(at("/station.pls")))
        assertEquals(at("/radio.aac"), resolver.resolve(at("/station.m3u")))
    }

    @Test
    fun `a playlist file that names another one is followed, and a redirect too`() {
        assertEquals(at("/icecast.mp3"), resolver.resolve(at("/two-steps.m3u")))
        assertEquals(at("/icecast.mp3"), resolver.resolve(at("/moved.pls")))
    }

    @Test
    fun `a playlist file that points to itself stops with an error after one call`() {
        assertTrue(refused("/self.pls").contains("back to itself"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `two playlist files that point to each other stop with an error`() {
        assertTrue(refused("/ring-a.m3u").contains("back to itself"))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a long row of playlist files stops at the limit`() {
        assertTrue(refused("/deep-1.m3u").contains("point to one another"))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `a redirect that never ends stops with an error`() {
        assertThrows(IOException::class.java) { resolver.resolve(at("/moves-for-ever.pls")) }
        assertTrue(server.requestCount <= MOST_REDIRECTS + 1)
    }

    @Test
    fun `a file that is too large is refused`() {
        assertTrue(refused("/big.pls").contains("larger than"))
    }

    @Test
    fun `a file path is never followed, as an entry or as a redirect`() {
        assertTrue(refused("/files-only.m3u").contains("no http or https stream"))
        assertTrue(refused("/moved-to-a-file.pls").contains("302"))
        assertEquals(at("/icecast.mp3"), resolver.resolve(at("/file-first.m3u")))
    }

    @Test
    fun `an empty file and a missing file are refused`() {
        assertTrue(refused("/empty.pls").contains("no http or https stream"))
        assertTrue(refused("/missing.pls").contains("404"))
    }

    private companion object {
        /** The HTTP client follows this many redirects and then gives up. */
        const val MOST_REDIRECTS = 20
    }
}
