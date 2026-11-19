/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistDaoTest {
    private val database = testDatabase()
    private val playlists = database.playlistDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `songs are added at the end and the same song can be added twice`() = runTest {
        val playlist = database.addPlaylist("Mine")
        val a = database.addSong("a")
        val b = database.addSong("b")

        listOf(a, b, a).forEach { playlists.addItem(playlist, it, at = 1) }

        assertEquals(listOf(a, b, a), playlists.items(playlist, 10, 0).map { it.row.song.id })
        assertEquals(listOf(b), playlists.items(playlist, limit = 1, offset = 1).map { it.row.song.id })
    }

    @Test
    fun `places can be put in a new order and taken out one by one`() = runTest {
        val playlist = database.addPlaylist("Mine")
        val a = database.addSong("a")
        val b = database.addSong("b")
        val first = playlists.addItem(playlist, a, at = 1)
        val second = playlists.addItem(playlist, b, at = 1)
        val third = playlists.addItem(playlist, a, at = 1)

        playlists.setItemOrder(playlist, listOf(third, second, first))
        playlists.removeItem(third)

        assertEquals(listOf(second, first), playlists.items(playlist, 10, 0).map { it.itemId })
    }

    @Test
    fun `an order for one playlist cannot move places of another`() = runTest {
        val mine = database.addPlaylist("Mine")
        val other = database.addPlaylist("Other")
        val otherFirst = playlists.addItem(other, database.addSong("a"), at = 1)
        val otherSecond = playlists.addItem(other, database.addSong("b"), at = 1)

        playlists.setItemOrder(mine, listOf(otherSecond, otherFirst))

        assertEquals(listOf(otherFirst, otherSecond), playlists.items(other, 10, 0).map { it.itemId })
    }

    @Test
    fun `playlists are listed by name with a count, and can be renamed`() = runTest {
        val b = database.addPlaylist("beta")
        val a = database.addPlaylist("Alpha")
        playlists.addItem(a, database.addSong("s"), at = 1)

        playlists.renamePlaylist(b, "Gamma", at = 7)

        val rows = playlists.playlists()
        assertEquals(listOf("Alpha" to 1, "Gamma" to 0), rows.map { it.playlist.name to it.songCount })
        assertEquals(7L, rows.last().playlist.updatedAt)
    }

    @Test
    fun `removing a playlist removes its places and keeps the songs`() = runTest {
        val playlist = database.addPlaylist("Mine")
        playlists.addItem(playlist, database.addSong("s"), at = 1)

        playlists.deletePlaylist(playlist)

        assertTrue(playlists.playlists().isEmpty())
        assertTrue(playlists.items(playlist, 10, 0).isEmpty())
        assertEquals(1, database.libraryDao().songsByTitle(10, 0).size)
    }

    @Test
    fun `a song whose file is gone is left out and gets its place back when the file returns`() = runTest {
        val playlist = database.addPlaylist("Mine")
        val a = database.addSong("a")
        val b = database.addSong("b")
        listOf(a, b).forEach { playlists.addItem(playlist, it, at = 1) }

        database.libraryDao().markMissing(listOf(a), at = 1)

        assertEquals(listOf(b), playlists.items(playlist, 10, 0).map { it.row.song.id })
        assertEquals(1, playlists.playlists().single().songCount)

        database.addSong("a")

        assertEquals(listOf(a, b), playlists.items(playlist, 10, 0).map { it.row.song.id })
    }
}
