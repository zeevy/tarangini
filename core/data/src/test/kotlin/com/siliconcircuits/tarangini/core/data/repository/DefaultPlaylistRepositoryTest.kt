/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.domain.model.Page
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultPlaylistRepositoryTest {
    private val database = testDatabase()
    private var now = 1_000L
    private val playlists = DefaultPlaylistRepository(database.playlistDao()) { now++ }

    @After
    fun closeDatabase() = database.close()

    private suspend fun playlist(id: Long) = playlists.playlists().firstOrNull { it.id == id }

    private suspend fun titles(playlistId: Long) = playlists.songs(playlistId, Page(100)).map { it.song.title }

    private suspend fun filled(): Long {
        val id = requireNotNull(playlists.create("Drive"))
        listOf("One", "Two", "Three").forEachIndexed { index, title ->
            playlists.add(id, database.addSong(title, sourceId = "${index + 1}"))
        }
        return id
    }

    @Test
    fun `a playlist is made, named again and taken away, and a bad name is refused`() = runTest {
        val id = requireNotNull(playlists.create("  Morning  "))

        assertEquals(listOf("Morning"), playlists.playlists().map { it.name })
        assertNull(playlists.create("   "))
        assertNull(playlists.create("x".repeat(101)))
        assertFalse(playlists.rename(id, ""))
        assertTrue(playlists.rename(id, "Evening"))
        assertEquals("Evening", playlist(id)?.name)

        playlists.delete(id)

        assertEquals(emptyList<String>(), playlists.playlists().map { it.name })
    }

    @Test
    fun `songs keep the order they were added in, the same song can be there twice, and a place can go`() = runTest {
        val id = filled()
        val one = playlists.songs(id, Page(100)).first()
        playlists.add(id, one.song.id)

        assertEquals(listOf("One", "Two", "Three", "One"), titles(id))
        assertEquals(4, playlist(id)?.songCount)

        playlists.remove(one.itemId)

        assertEquals(listOf("Two", "Three", "One"), titles(id))
    }

    @Test
    fun `a place moves before another place or to the end`() = runTest {
        val id = filled()
        val (one, two, three) = playlists.songs(id, Page(100)).map { it.itemId }

        playlists.move(id, three, beforeItemId = one)
        val toFront = titles(id)
        playlists.move(id, three, beforeItemId = null)
        val toEnd = titles(id)
        playlists.move(id, one, beforeItemId = 999)
        playlists.move(id, 999, beforeItemId = two)

        assertEquals(listOf("Three", "One", "Two"), toFront)
        assertEquals(listOf("One", "Two", "Three"), toEnd)
        assertEquals(listOf("Two", "Three", "One"), titles(id))
    }

    @Test
    fun `a song or a playlist that is gone adds nothing`() = runTest {
        val id = filled()
        val song = playlists.songs(id, Page(1)).single().song.id

        playlists.add(id, 999)
        playlists.add(999, song)

        assertEquals(listOf("One", "Two", "Three"), titles(id))
    }

    @Test
    fun `a playlist with 5000 songs is read in pages and reordered`() = runTest {
        val id = requireNotNull(playlists.create("Long"))
        val song = database.addSong("Song", sourceId = "1")
        repeat(5_000) { playlists.add(id, song) }
        val last = playlists.songs(id, Page(limit = 1, offset = 4_999)).single().itemId
        val first = playlists.songs(id, Page(1)).single().itemId

        playlists.move(id, last, beforeItemId = first)

        assertEquals(last, playlists.songs(id, Page(1)).single().itemId)
        assertEquals(5_000, playlist(id)?.songCount)
    }
}
