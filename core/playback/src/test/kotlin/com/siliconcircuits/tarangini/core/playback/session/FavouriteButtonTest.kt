/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import androidx.media3.session.CommandButton
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSongLookup
import com.siliconcircuits.tarangini.core.testing.library.testSong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FavouriteButtonTest {
    private val songs = listOf(testSong(1), testSong(2))
    private val favourites = FakeFavouriteRepository().apply { known = songs }
    private val shown = mutableListOf<List<CommandButton>>()

    // The button works in the background scope, which runs only what is due now when asked.
    private fun TestScope.button() =
        FavouriteButton(backgroundScope, favourites, FakeSongLookup(songs)) { if (it) "Remove" else "Add" }
            .also { it.start(shown::add) }

    private fun names() = shown.map { buttons -> buttons.map { it.displayName.toString() } }

    @Test
    fun `there is no button while nothing plays, and for an item that is no song of the library`() = runTest {
        val button = button()
        runCurrent()
        button.nowPlaying("https://radio.example/stream")
        runCurrent()
        button.toggle()
        runCurrent()

        assertEquals(listOf(emptyList<String>()), names())
        assertEquals(emptyList<Long>(), favourites.ids.value)
    }

    @Test
    fun `the button follows the song that plays and its mark`() = runTest {
        favourites.setFavourite(2, true)
        val button = button()

        button.nowPlaying(songs[0].mediaUri)
        runCurrent()
        button.nowPlaying(songs[1].mediaUri)
        runCurrent()

        assertEquals(listOf(listOf("Add"), listOf("Remove")), names().takeLast(2))
        assertEquals(FavouriteButton.COMMAND, shown.last().single().sessionCommand)
    }

    @Test
    fun `a press marks the song that plays, and a second press takes the mark away`() = runTest {
        val button = button()
        button.nowPlaying(songs[0].mediaUri)
        runCurrent()

        button.toggle()
        runCurrent()
        val afterFirst = favourites.ids.value
        button.toggle()
        runCurrent()

        assertEquals(listOf(1L), afterFirst)
        assertEquals(emptyList<Long>(), favourites.ids.value)
        assertEquals(listOf("Add", "Remove", "Add"), names().takeLast(3).map { it.single() })
    }
}
