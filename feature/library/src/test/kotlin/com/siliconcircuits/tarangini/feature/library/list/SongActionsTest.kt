/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.list

import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SongActionsTest {
    private val playback = FakePlaybackController()
    private val favourites = FakeFavouriteRepository()
    private val actions = SongActions(playback, FakePlaybackQueue(), favourites, FakePlaylistRepository())
    private val songs = listOf(testSong(1), testSong(2), testSong(3))

    private suspend fun slowRead(page: Page): List<Song> {
        delay(100)
        return songs.drop(page.offset).take(page.limit)
    }

    @Test
    fun `a second tap takes the place of a first one that still reads the list`() = runTest {
        val rows = actions.rowActions(backgroundScope, read = ::slowRead)

        rows.onPlay(songs[0])
        rows.onPlay(songs[2])
        advanceTimeBy(1_000)

        assertEquals(listOf("play [test:1, test:2, test:3] from 2"), playback.commands)
    }

    @Test
    fun `a song that left the list plays alone`() = runTest {
        val rows = actions.rowActions(backgroundScope, read = ::slowRead)

        rows.onPlay(testSong(9))
        advanceTimeBy(1_000)

        assertEquals(listOf("play [test:9] from 0"), playback.commands)
    }

    @Test
    fun `a list that cannot be read plays the song alone`() = runTest {
        val rows = actions.rowActions(backgroundScope) { error("disk full") }

        rows.onPlay(songs[1])
        advanceTimeBy(1_000)

        assertEquals(listOf("play [test:2] from 0"), playback.commands)
    }

    @Test
    fun `a row can mark its song as a favourite, and every row sees the mark`() = runTest {
        val rows = actions.rowActions(backgroundScope, read = ::slowRead)
        val otherList = actions.rowActions(backgroundScope, read = ::slowRead)

        rows.onSetFavourite(songs[1], true)
        advanceTimeBy(1_000)
        val marked = otherList.favourites.value
        rows.onSetFavourite(songs[1], false)
        advanceTimeBy(1_000)

        assertEquals(setOf(2L), marked)
        assertEquals(emptySet<Long>(), otherList.favourites.value)
    }
}
