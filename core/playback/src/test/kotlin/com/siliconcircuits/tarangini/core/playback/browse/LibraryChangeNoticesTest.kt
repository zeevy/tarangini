/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlayHistory
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Moving the test clock by hand is API of the coroutines test library that may still change.
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryChangeNoticesTest {
    private val library = FakeLibraryRepository()
    private val favourites = FakeFavouriteRepository()
    private val history = FakePlayHistory()
    private val playlists = FakePlaylistRepository()
    private val personal = PersonalLists(favourites, history, playlists)
    private val told = mutableListOf<String>()

    @Test
    fun `nothing is told at the start, and each open list is told once after the library changed`() = runTest {
        val notices =
            LibraryChangeNotices(library, personal, backgroundScope, {
                listOf("library", "songs")
            }, told::add)
        notices.start()
        pass()
        assertTrue(told.isEmpty())

        library.version.value++
        pass()

        assertEquals(listOf("library", "songs"), told)
    }

    @Test
    fun `many changes in a short time are told once, after the last one`() = runTest {
        val notices =
            LibraryChangeNotices(library, personal, backgroundScope, { listOf("songs") }, told::add)
        notices.start()
        runCurrent()

        repeat(5) {
            library.version.value++
            advanceTimeBy(HALF_A_SECOND)
        }
        assertTrue(told.isEmpty())
        pass()

        assertEquals(listOf("songs"), told)
    }

    @Test
    fun `nothing is told after the end`() = runTest {
        val notices =
            LibraryChangeNotices(library, personal, backgroundScope, { listOf("songs") }, told::add)
        notices.start()
        runCurrent()

        notices.end()
        library.version.value++
        pass()

        assertTrue(told.isEmpty())
    }

    /** Lets more time pass than the wait after a change. The watch runs in the background, which "until idle" skips. */
    private fun TestScope.pass() {
        advanceTimeBy(LONGER_THAN_THE_WAIT)
        runCurrent()
    }

    private companion object {
        const val HALF_A_SECOND = 500L
        const val LONGER_THAN_THE_WAIT = 5_000L
    }

    @Test
    fun `only the favourites list is told after a favourite was marked`() = runTest {
        val open = { listOf("songs", "favourites") }
        val notices = LibraryChangeNotices(library, personal, backgroundScope, open, told::add)
        notices.start()
        pass()

        favourites.setFavourite(1, true)
        pass()

        assertEquals(listOf("favourites"), told)
    }

    @Test
    fun `only the playlists are told after a playlist changed`() = runTest {
        val open = { listOf("songs", "playlists", "playlist.1", "recent") }
        val notices = LibraryChangeNotices(library, personal, backgroundScope, open, told::add)
        notices.start()
        pass()

        playlists.create("Drive")
        pass()

        assertEquals(listOf("playlists", "playlist.1"), told)
    }

    @Test
    fun `only the recent and the most played list are told after a play was counted`() = runTest {
        val open = { listOf("songs", "recent", "mostplayed") }
        val notices = LibraryChangeNotices(library, personal, backgroundScope, open, told::add)
        notices.start()
        pass()

        history.songPlayed(1)
        pass()

        assertEquals(listOf("recent", "mostplayed"), told)
    }
}
