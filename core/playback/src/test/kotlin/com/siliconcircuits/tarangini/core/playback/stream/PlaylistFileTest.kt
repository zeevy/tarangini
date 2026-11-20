/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistFileTest {
    @Test
    fun `a pls and an m3u address are playlist files, whatever the case and the query`() {
        listOf("http://a.example/listen.pls", "https://a.example/x/LISTEN.M3U", "http://a.example/listen.pls?sid=1")
            .forEach { assertTrue(it, it.toHttpUrl().isPlaylistFile()) }
    }

    @Test
    fun `a stream, an HLS address and an address that only has the ending in its query are not`() {
        listOf(
            "http://a.example/stream",
            "http://a.example/live.m3u8",
            "http://a.example/radio.mp3",
            "http://a.example/",
            "http://a.example/stream?file=a.pls",
        ).forEach { assertFalse(it, it.toHttpUrl().isPlaylistFile()) }
    }

    @Test
    fun `a pls file gives its File lines in order and nothing else`() {
        val pls = """
            [playlist]
            numberofentries=2
            File1=http://a.example/one
            Title1=One = the first
            Length1=-1
            file2 = http://a.example/two?x=1
            Version=2
        """.trimIndent()

        assertEquals(listOf("http://a.example/one", "http://a.example/two?x=1"), playlistEntries(pls))
    }

    @Test
    fun `other lines of a pls file are never taken for an address, wherever they stand`() {
        val pls = "[playlist]\nGenre=Rock\nlength=5\nTitle1=http://other.example/\nFile1=http://a.example/one\n"

        assertEquals(listOf("http://a.example/one"), playlistEntries(pls))
    }

    @Test
    fun `an m3u file gives its lines without the comments, also with Windows line ends and a byte order mark`() {
        val m3u = "\uFEFF#EXTM3U\r\n#EXTINF:-1,Station\r\nhttp://a.example/stream?id=7\r\n\r\n  relative/stream  \r\n"

        assertEquals(listOf("http://a.example/stream?id=7", "relative/stream"), playlistEntries(m3u))
    }

    @Test
    fun `a file with no address gives nothing`() {
        assertEquals(emptyList<String>(), playlistEntries("[playlist]\nNumberOfEntries=0\nFile1=\n"))
        assertEquals(emptyList<String>(), playlistEntries(""))
    }
}
