/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local

import com.siliconcircuits.swaratarang.core.data.local.entity.FavouriteSongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouriteDaoTest {
    private val database = testDatabase()
    private val favourites = database.favouriteDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `favourite songs come newest first and a second add changes nothing`() = runTest {
        val first = database.addSong("First")
        val second = database.addSong("Second")
        favourites.addSong(FavouriteSongEntity(first, addedAt = 1))
        favourites.addSong(FavouriteSongEntity(second, addedAt = 2))

        favourites.addSong(FavouriteSongEntity(first, addedAt = 3))

        assertEquals(listOf(second, first), favourites.favouriteSongs(10, 0).map { it.song.id })
        assertEquals(listOf(first), favourites.favouriteSongs(limit = 1, offset = 1).map { it.song.id })
    }

    @Test
    fun `taking a song out of the favourites keeps the song`() = runTest {
        val id = database.addSong("Song")
        favourites.addSong(FavouriteSongEntity(id, addedAt = 1))
        assertTrue(favourites.isFavouriteSong(id).first())

        favourites.removeSong(id)

        assertFalse(favourites.isFavouriteSong(id).first())
        assertEquals(1, database.libraryDao().songsByTitle(10, 0).size)
    }

    @Test
    fun `favourite stations keep the order they were added in until the user changes it`() = runTest {
        val a = database.addStation("A")
        val b = database.addStation("B")
        val c = database.addStation("C")
        listOf(a, b, c, a).forEach { favourites.addStation(it) }
        assertEquals(listOf(a, b, c), favourites.favouriteStations().map { it.id })

        favourites.setStationOrder(listOf(c, a, b))

        assertEquals(listOf(c, a, b), favourites.favouriteStations().map { it.id })
    }

    @Test
    fun `a station taken out of the favourites stays saved, and the next add goes to the end`() = runTest {
        val a = database.addStation("A")
        val b = database.addStation("B")
        listOf(a, b).forEach { favourites.addStation(it) }

        favourites.removeStation(a)
        assertFalse(favourites.isFavouriteStation(a).first())
        favourites.addStation(a)

        assertEquals(listOf(b, a), favourites.favouriteStations().map { it.id })
        assertEquals("A", database.stationDao().station(a)?.name)
    }
}
