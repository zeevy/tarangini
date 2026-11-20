/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.siliconcircuits.tarangini.core.playback.di.HttpClientModule
import java.io.IOException
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** One station of each kind is opened through the data source of the player, against a local server. */
// The data source classes of Media3 are API that may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class StreamDataSourceTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val server: MockWebServer = stationServer()
    private val factory = streamDataSourceFactory(context) { OkHttpClient() }

    @After
    fun stopServer() = server.close()

    private fun address(path: String): Uri = Uri.parse(server.url(path).toString())

    /** Opens the address as the player does and gives all that the server sends. */
    private fun read(uri: Uri): String {
        val source = factory.createDataSource()
        try {
            source.open(DataSpec(uri))
            return String(DataSourceUtil.readToEnd(source))
        } finally {
            source.close()
        }
    }

    @Test
    fun `an Icecast, a Shoutcast and an AAC stream are read`() {
        assertEquals("icecast", read(address("/icecast.mp3")))
        assertEquals("shoutcast", read(address("/shoutcast")))
        assertEquals("aac", read(address("/radio.aac")))
    }

    @Test
    fun `a pls and an m3u file are exchanged for the stream they name`() {
        assertEquals("icecast", read(address("/station.pls")))
        assertEquals("aac", read(address("/station.m3u")))
    }

    @Test
    fun `an HLS address is read by the HLS part of the player, the others as one stream`() {
        val sources = DefaultMediaSourceFactory(context).setDataSourceFactory(factory)

        assertEquals("#EXTM3U", read(address("/live.m3u8")))
        assertTrue(sources.createMediaSource(MediaItem.fromUri(address("/live.m3u8"))) is HlsMediaSource)
        listOf("/icecast.mp3", "/shoutcast", "/radio.aac", "/station.pls", "/station.m3u").forEach {
            assertTrue(it, sources.createMediaSource(MediaItem.fromUri(address(it))) is ProgressiveMediaSource)
        }
    }

    @Test
    fun `an address that is not an audio file of the phone or a stream is never opened`() {
        val refused = listOf(
            "file:///data/data/com.siliconcircuits.tarangini/databases/tarangini.db",
            "content://com.other.app.provider/secret",
            "content://media/external/images/media/1",
            "asset:///a.mp3",
            "data:audio/mpeg;base64,AAAA",
            "udp://239.0.0.1:1234",
            "http://user:secret@radio.example/stream",
        )

        refused.forEach { uri ->
            assertThrows(uri, RefusedAddressException::class.java) { read(Uri.parse(uri)) }
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `an audio file of the phone is still let through to the files of the phone`() {
        // No such file is there in a test, so the read fails, but not with a refusal.
        val error = assertThrows(IOException::class.java) { read(Uri.parse("content://media/external/audio/media/1")) }

        assertFalse(error is RefusedAddressException)
    }

    @Test
    fun `the shared client says which app asks`() {
        val shared = HttpClientModule.httpClient(context)
        val named = streamDataSourceFactory(context) { shared }.createDataSource()
        try {
            named.open(DataSpec(address("/station.pls")))
        } finally {
            named.close()
        }

        repeat(2) { assertTrue(server.takeRequest().headers["User-Agent"].orEmpty().startsWith("Tarangini/")) }
    }
}
