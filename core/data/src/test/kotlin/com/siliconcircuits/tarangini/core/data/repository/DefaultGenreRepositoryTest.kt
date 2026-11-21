/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.sortKey
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.domain.model.Page
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultGenreRepositoryTest {
    private val database = testDatabase()
    private val genres = DefaultGenreRepository(database.genreDao(), database.libraryDao())

    @After
    fun close() = database.close()

    private suspend fun genresOf(songId: Long, vararg names: String) {
        val dao = database.genreDao()
        dao.setGenres(songId, names.map { dao.saveGenre(it, sortKey(it)) })
    }

    @Test
    fun `each song has the keys of its named genres, and a song with no genre tag has none`() = runTest {
        val one = database.addSong("One")
        val two = database.addSong("Two")
        val three = database.addSong("Three")
        genresOf(one, "Rock", "Pop")
        genresOf(two, "rock")
        genresOf(three, "")

        val keys = genres.genreKeysOf(listOf(one, two, three, one))

        assertEquals(mapOf(one to listOf(sortKey("Pop"), sortKey("Rock")), two to listOf(sortKey("Rock"))), keys)
    }

    @Test
    fun `a genre of the list has the same key a song has`() = runTest {
        val one = database.addSong("One")
        genresOf(one, "Carnatic")

        val listed = genres.genres(Page(limit = 10, offset = 0)).single()

        assertEquals(genres.genreKeysOf(listOf(one))[one], listOf(listed.key))
    }

    @Test
    fun `many songs are read in parts`() = runTest {
        val ids = (1..1_200).map { database.addSong("Song $it") }
        genresOf(ids.last(), "Jazz")

        assertEquals(mapOf(ids.last() to listOf(sortKey("Jazz"))), genres.genreKeysOf(ids))
    }
}
