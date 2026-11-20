/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.data.mediastore.localSourceIdOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultSongLookupTest {
    private val database = testDatabase()
    private val lookup = DefaultSongLookup(database.libraryDao())

    @After
    fun closeDatabase() = database.close()

    private suspend fun song(title: String, artist: String, fileId: Int) =
        database.addSong(title, artist, "Album", "$fileId", null, null, null, 180_000, 0)

    private fun uri(fileId: Int) = "content://media/external/audio/media/$fileId"

    @Test
    fun `songs are found by their addresses, and an address that is not known is left out`() = runTest {
        song("a", "Zed", fileId = 1)
        song("b", "Mia", fileId = 2)

        val found = lookup.byMediaUri(
            listOf(uri(2), uri(1), uri(1), uri(99), "content://media/internal/audio/media/1", "file:///a.mp3", ""),
        )

        assertEquals(setOf(uri(1), uri(2)), found.keys)
        assertEquals("a", found[uri(1)]?.title)
        assertEquals("Zed", found[uri(1)]?.artist)
        assertEquals("b", found[uri(2)]?.title)
    }

    @Test
    fun `only the address of a file on the shared storage names a file id`() {
        assertEquals("7", localSourceIdOf(uri(7)))
        assertEquals("007", localSourceIdOf("content://media/external/audio/media/007"))
        listOf(
            "content://media/internal/audio/media/7",
            "content://media/external/audio/media/7?x=1",
            "content://media/external/audio/media/7/albumart",
            "content://media/external/audio/media/",
            "content://media/external/audio/media/abc",
            "content://media/external/video/media/7",
            "file:///music/7",
            "7",
            "",
        ).forEach { assertNull(it, localSourceIdOf(it)) }
    }

    @Test
    fun `a list as long as a queue is read in parts`() = runTest {
        repeat(501) { song("song $it", "Artist", fileId = it + 1) }

        assertEquals(501, lookup.byMediaUri(List(1_200) { uri(it + 1) }).size)
    }
}
