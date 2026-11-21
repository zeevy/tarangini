/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlayHistory
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val songs = listOf(testSong(1, "One"), testSong(2, "Two"), testSong(3, "Three"))
    private val playback = FakePlaybackController()
    private val queue = FakePlaybackQueue()
    private val history = FakePlayHistory().apply { known = songs }
    private val playlists = FakePlaylistRepository().apply { known = songs }
    private val favourites = FakeFavouriteRepository()
    private val item = PlaybackItem("test:1", "One", "Ravi", "Album", null)

    private fun TestScope.watched(): HomeViewModel =
        HomeViewModel(playback, queue, history, playlists, favourites, FakeLibraryRepository()).also { viewModel ->
            backgroundScope.launch { viewModel.state.collect() }
            runCurrent()
        }

    @Test
    fun `a new install is the first use, with no section`() = runTest(mainDispatcher.dispatcher) {
        val state = watched().state.value

        assertTrue(state.isFirstUse)
        assertEquals(HomeUiState(isLoading = false), state)
    }

    @Test
    fun `the songs heard last and the playlists show, and show again after a change`() =
        runTest(mainDispatcher.dispatcher) {
            history.songPlayed(1)
            history.songPlayed(2)
            playlists.create("Drive")
            val viewModel = watched()
            val first = viewModel.state.value

            history.songPlayed(3)
            playlists.create("Morning")
            runCurrent()

            assertFalse(first.isFirstUse)
            assertEquals(
                listOf("Two", "One") to listOf("Drive"),
                first.recent.map { it.title } to first.playlists.map { it.name },
            )
            assertEquals(listOf("Three", "Two", "One"), viewModel.state.value.recent.map { it.title })
            assertEquals(listOf("Drive", "Morning"), viewModel.state.value.playlists.map { it.name })
        }

    @Test
    fun `the loaded item shows only while it does not play, and its button goes on with it`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = watched()
            playback.state.value = PlaybackState(queue = listOf(item), currentIndex = 0)
            runCurrent()
            val paused = viewModel.state.value.paused

            viewModel.resume()
            playback.state.value = playback.state.value.copy(isPlaying = true, playWhenReady = true)
            runCurrent()

            assertEquals(item, paused)
            assertNull(viewModel.state.value.paused)
            assertEquals(listOf("resume"), playback.commands)
        }

    @Test
    fun `a tap on a recent song plays the recent songs from that song on`() = runTest(mainDispatcher.dispatcher) {
        listOf(1L, 2L, 3L).forEach { history.songPlayed(it) }
        val viewModel = watched()

        viewModel.songActions.onPlay(songs[1])
        viewModel.songActions.onSetFavourite(songs[0], true)
        runCurrent()

        assertEquals(listOf("play [test:3, test:2, test:1] from 1"), playback.commands)
        assertEquals(listOf(1L), favourites.ids.value)
    }

    @Test
    fun `most played shows once a song was heard more than once, and a tap plays that list`() =
        runTest(mainDispatcher.dispatcher) {
            listOf(1L, 2L, 3L).forEach { history.songPlayed(it) }
            val viewModel = watched()
            val heardOnce = viewModel.state.value.mostPlayed

            listOf(1L, 1L, 2L).forEach { history.songPlayed(it) }
            runCurrent()
            viewModel.mostPlayedActions.onPlay(songs[1])

            // While every song was heard once the list would repeat the recent songs.
            assertEquals(emptyList<String>(), heardOnce.map { it.title })
            assertEquals(listOf("One", "Two", "Three"), viewModel.state.value.mostPlayed.map { it.title })
            assertEquals(listOf("play [test:1, test:2, test:3] from 1"), playback.commands)
        }
}
