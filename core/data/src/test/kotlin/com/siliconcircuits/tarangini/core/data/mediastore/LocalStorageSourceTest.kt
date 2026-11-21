/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.domain.premium.ByteRange
import com.siliconcircuits.tarangini.core.domain.premium.StorageEntry
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalStorageSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val bytes = ByteArray(1_000) { it.toByte() }
    private val audio = FakeAudioFileSource(
        listOf(
            audioFile("1", fileName = "b.mp3", folderPath = "external_primary/Music"),
            audioFile("2", fileName = "A.mp3", folderPath = "external_primary/Music"),
            audioFile("3", fileName = "c.mp3", folderPath = "external_primary/Music/Rock"),
            audioFile("4", fileName = "d.mp3", folderPath = "external_primary/Music/Jazz/Live"),
            audioFile("5", fileName = "e.mp3", folderPath = "1234-ABCD/Songs"),
        ),
    )

    private fun source(opener: AudioFileOpener = AudioFileOpener { ByteArrayInputStream(bytes) }) =
        LocalStorageSource(audio, opener, StandardTestDispatcher())

    @Test
    fun `the top has a folder per storage`() = runTest {
        val top = LocalStorageSource(audio, {
            ByteArrayInputStream(bytes)
        }, StandardTestDispatcher(testScheduler)).list()

        assertEquals(listOf("1234-ABCD", "external_primary"), top.map { it.name })
        assertEquals(true, top.all { it.isFolder })
    }

    @Test
    fun `a folder lists its folders first, then its files, each by name`() = runTest {
        val music = LocalStorageSource(audio, { ByteArrayInputStream(bytes) }, StandardTestDispatcher(testScheduler))
            .list("external_primary/Music")

        assertEquals(
            listOf(
                StorageEntry("external_primary/Music/Jazz", "Jazz", isFolder = true),
                StorageEntry("external_primary/Music/Rock", "Rock", isFolder = true),
                StorageEntry("2", "A.mp3", isFolder = false, sizeBytes = 1),
                StorageEntry("1", "b.mp3", isFolder = false, sizeBytes = 1),
            ),
            music.map { if (it.isFolder) it else it.copy(sizeBytes = 1) },
        )
    }

    @Test
    fun `a folder with a slash at the end is the same folder`() = runTest {
        val local = LocalStorageSource(audio, { ByteArrayInputStream(bytes) }, StandardTestDispatcher(testScheduler))

        assertEquals(local.list("external_primary/Music"), local.list("external_primary/Music/"))
    }

    @Test
    fun `the length limit holds for skip and available too`() {
        source().open("7", ByteRange(offset = 0, length = 10)).use { part ->
            assertEquals(10, part.available())
            assertEquals(10L, part.skip(500))
            assertEquals(-1, part.read())
        }
    }

    @Test
    fun `without the permission, or for a folder it does not know, the list is empty`() = runTest {
        val local = LocalStorageSource(audio, { ByteArrayInputStream(bytes) }, StandardTestDispatcher(testScheduler))

        assertEquals(emptyList<StorageEntry>(), local.list("external_primary/Podcasts"))
        audio.permission = false
        assertEquals(emptyList<StorageEntry>(), local.list())
    }

    @Test
    fun `a range gives the bytes from its offset, at most its length`() {
        val part = source().open("7", ByteRange(offset = 100, length = 50)).use { it.readBytes() }

        assertArrayEquals(bytes.copyOfRange(100, 150), part)
    }

    @Test
    fun `a file of the phone moves to the offset at once, and the whole rest is read with no length`() {
        val file = folder.newFile("song.mp3").apply { writeBytes(bytes) }

        val rest = source { FileInputStream(file) }.open("7", ByteRange(offset = 990)).use { it.readBytes() }

        assertArrayEquals(bytes.copyOfRange(990, 1_000), rest)
    }

    @Test
    fun `an offset after the end gives nothing`() {
        assertEquals(0, source().open("7", ByteRange(offset = 5_000)).use { it.readBytes() }.size)
    }

    @Test(expected = FileNotFoundException::class)
    fun `a path that is no id of the phone is refused`() {
        source().open("../etc/passwd")
    }

    @Test(expected = FileNotFoundException::class)
    fun `a file that is gone is refused`() {
        source { throw FileNotFoundException("gone") }.open("7")
    }
}
