/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.lyrics

import android.net.Uri
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * The files in `lyrics/` are the first gapless test files with lyrics added to their tags: an ID3
 * frame in UTF-16 for the MP3 file, and a lyrics item at the end of the file for the M4A file.
 */
@RunWith(RobolectricTestRunner::class)
class SongLyricsTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun bytesOf(file: String): ByteArray =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(file)).use { it.readBytes() }

    private fun lyricsIn(file: String): String? {
        val bytes = bytesOf(file)
        return TagReader { ByteArrayInputStream(bytes) }.metadata()?.let(LyricsFrames::textOf)
    }

    @Test
    fun `the lyrics of an MP3 file are read from its tag`() {
        assertEquals(LYRICS, lyricsIn("lyrics/with-lyrics.mp3"))
    }

    @Test
    fun `the lyrics of an M4A file are read, also when the tags are at the end of the file`() {
        assertEquals(LYRICS, lyricsIn("lyrics/with-lyrics.m4a"))
    }

    @Test
    fun `a file with no lyrics, and a file that is no music, have none`() {
        assertNull(lyricsIn("gapless/with-delay-header-1.mp3"))
        assertNull(lyricsIn("gapless/with-edit-list-skip-1.m4a"))
        assertNull(TagReader { ByteArrayInputStream("no music at all".repeat(500).toByteArray()) }.metadata())
        assertNull(TagReader { ByteArrayInputStream(ByteArray(0)) }.metadata())
        assertNull(TagReader { null }.metadata())
    }

    @Test
    fun `a lyrics frame is read in each encoding a tag can have`() {
        val latin = byteArrayOf(0) + "eng".toByteArray() + byteArrayOf(0) + "Café".toByteArray(Charsets.ISO_8859_1)
        val utf8 = byteArrayOf(3) + "eng".toByteArray() + "note".toByteArray() + byteArrayOf(0) + "గానం".toByteArray()
        val bigEndian = byteArrayOf(2) + "eng".toByteArray() + byteArrayOf(0, 0) + "गीत".toByteArray(Charsets.UTF_16BE)

        assertEquals("Café", LyricsFrames.unsynced(latin))
        assertEquals("గానం", LyricsFrames.unsynced(utf8))
        assertEquals("गीत", LyricsFrames.unsynced(bigEndian))
    }

    @Test
    fun `bytes that are no lyrics frame give nothing`() {
        assertNull(LyricsFrames.unsynced(ByteArray(0)))
        assertNull(LyricsFrames.unsynced(byteArrayOf(1, 2)))
        assertNull(LyricsFrames.unsynced(byteArrayOf(9) + "eng".toByteArray() + byteArrayOf(0) + "x".toByteArray()))
        // No zero character ends the description.
        assertNull(LyricsFrames.unsynced(byteArrayOf(0) + "engabc".toByteArray()))
        assertNull(LyricsFrames.unsynced(byteArrayOf(1) + "eng".toByteArray() + byteArrayOf(0)))
    }

    @Test
    fun `lyrics are cleaned and cut before they are shown`() {
        assertEquals("one\ntwo\nthree four", LyricsFrames.clean("  one\r\ntwo\rthree\tfour\u0007\u202E \n"))
        // The joiner is not seen, but Malayalam needs it for this letter. It stays.
        assertEquals("അവന\u0D4D\u200D", LyricsFrames.clean("അവന\u0D4D\u200D"))
        assertNull(LyricsFrames.clean(" \r\n\u0000 "))
        assertEquals(LyricsFrames.MAX_CHARS, LyricsFrames.clean("a".repeat(LyricsFrames.MAX_CHARS + 10))?.length)
    }

    @Test
    fun `a tag that states to be longer than the file is not read`() {
        // The four size bytes state 256 MiB. The file has a few bytes.
        val file = "ID3".toByteArray() + byteArrayOf(3, 0, 0, 0x7F, 0x7F, 0x7F, 0x7F) + ByteArray(64)

        assertNull(TagReader(file.size.toLong()) { ByteArrayInputStream(file) }.metadata())
    }

    @Test
    fun `only a song of the phone is opened`() = runTest {
        val lyrics = DefaultSongLyrics(context, Dispatchers.Unconfined)
        val song = Uri.parse("content://media/external/audio/media/7")
        val file = bytesOf("lyrics/with-lyrics.mp3")
        val files = shadowOf(context.contentResolver)
        // The reader opens the file once for each kind of file it tries, so each open gets a new stream.
        files.registerInputStreamSupplier(song) { ByteArrayInputStream(file) }
        val other = Uri.parse("content://com.example.files/secret")
        files.registerInputStreamSupplier(other) { ByteArrayInputStream(file) }

        assertEquals(LYRICS, lyrics.lyricsOf(song.toString()))
        assertNull(lyrics.lyricsOf(other.toString()))
        assertNull(lyrics.lyricsOf("https://example.com/stream.mp3"))
        assertNull(lyrics.lyricsOf("file:///data/data/app/files/x.mp3"))
        // A song whose file is gone.
        assertNull(lyrics.lyricsOf("content://media/external/audio/media/8"))
    }

    private companion object {
        const val LYRICS = "First line\nSecond line\n\nతరంగిణి"
    }
}
