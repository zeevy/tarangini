/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.domain.model.Page
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultFavouriteRepositoryTest {
    private val database = testDatabase()
    private var now = 1_000L
    private val favourites = DefaultFavouriteRepository(database.favouriteDao()) { now++ }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `favourites come with the one marked last first, in pages`() = runTest {
        val first = database.addSong("First", sourceId = "1")
        val second = database.addSong("Second", sourceId = "2")
        favourites.setFavourite(first, true)
        favourites.setFavourite(second, true)

        assertEquals(setOf(first, second), favourites.songIds().first())
        assertEquals(listOf("Second", "First"), favourites.songs(Page(10)).map { it.title })
        assertEquals(listOf("First"), favourites.songs(Page(limit = 1, offset = 1)).map { it.title })
    }

    @Test
    fun `a mark can be taken away, and a song that is not there is left alone`() = runTest {
        val song = database.addSong("Song", sourceId = "3")
        favourites.setFavourite(song, true)

        favourites.setFavourite(song, false)
        favourites.setFavourite(song + 100, true)
        favourites.setFavourite(song + 100, false)

        assertEquals(emptySet<Long>(), favourites.songIds().first())
        assertEquals(emptyList<String>(), favourites.songs(Page(10)).map { it.title })
    }
}
