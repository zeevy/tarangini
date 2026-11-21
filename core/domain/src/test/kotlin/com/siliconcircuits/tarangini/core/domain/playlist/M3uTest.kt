/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uTest {
    private val rock = SongFile(1, "external_primary/Music/Rock", "One.mp3")
    private val jazz = SongFile(2, "external_primary/Music/Jazz", "One.mp3")
    private val raga = SongFile(3, "external_primary/Music/Ragas", "Morning Raga.m4a")
    private val files = listOf(rock, jazz, raga)

    @Test
    fun `a written file is read back as its paths in order`() {
        val text = M3u.write(
            listOf(
                M3uSong("/storage/emulated/0/Music/Rock/One.mp3", "One", "Band", 185_400),
                M3uSong("/storage/emulated/0/Music/Ragas/Morning Raga.m4a", "Morning\nRaga", "Ravi", 60_000),
            ),
        )

        assertEquals("#EXTM3U", text.lines().first())
        assertEquals("#EXTINF:185,Band - One", text.lines()[1])
        assertEquals("#EXTINF:60,Ravi - Morning Raga", text.lines()[3])
        assertEquals(
            listOf("/storage/emulated/0/Music/Rock/One.mp3", "/storage/emulated/0/Music/Ragas/Morning Raga.m4a"),
            M3u.entries(text),
        )
        assertEquals(listOf(1L, 3L), M3u.entries(text).map { M3u.match(it, files) })
    }

    @Test
    fun `comments, empty lines, the byte order mark and lines that are too long are left out`() {
        val text = "\uFEFF#EXTM3U\r\n\r\n  a.mp3  \r\n#EXTINF:1,x\r\n" + "x".repeat(M3u.MAX_LINE + 1) + "\r\nb.mp3"

        assertEquals(listOf("a.mp3", "b.mp3"), M3u.entries(text))
    }

    @Test
    fun `no more lines than the limit are read`() {
        val text = (1..M3u.MAX_ENTRIES + 10).joinToString("\n") { "$it.mp3" }

        assertEquals(M3u.MAX_ENTRIES, M3u.entries(text).size)
    }

    @Test
    fun `a path of a Windows computer matches by its last folders`() {
        assertEquals(2L, M3u.match("C:\\Users\\me\\Music\\Jazz\\one.MP3", files))
    }

    @Test
    fun `a relative path matches, with its steps up and down worked out`() {
        assertEquals(1L, M3u.match("Rock/One.mp3", files))
        assertEquals(2L, M3u.match("../Music/./Rock/../Jazz/One.mp3", files))
    }

    @Test
    fun `a file address matches with its escaped characters`() {
        assertEquals(3L, M3u.match("file:///storage/emulated/0/Music/Ragas/Morning%20Raga.m4a", files))
    }

    @Test
    fun `a file name alone matches only when one song has it`() {
        assertEquals(3L, M3u.match("Morning Raga.m4a", files))
        assertEquals(3L, M3u.match("/other/place/Morning Raga.m4a", files))
        assertNull(M3u.match("One.mp3", files))
        assertNull(M3u.match("/other/place/One.mp3", files))
    }

    @Test
    fun `an address on the internet, an unknown file and a line with no name match nothing`() {
        assertNull(M3u.match("https://example.com/Music/Rock/One.mp3", files))
        assertNull(M3u.fileNameOf("http://example.com/a.mp3"))
        assertNull(M3u.match("Music/Rock/Two.mp3", files))
        assertNull(M3u.match("///", files))
        assertEquals("one.mp3", M3u.fileNameOf("C:\\Music\\One.mp3"))
    }
}
