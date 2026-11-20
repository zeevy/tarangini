/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.search

import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.domain.repository.SearchRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSearchRepository
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    // A standard dispatcher, so the test moves the time of the typing pause itself.
    @get:Rule
    val mainDispatcher = MainDispatcherRule(StandardTestDispatcher())

    private val search = FakeSearchRepository().apply {
        results = SearchResults(
            songs = listOf(testSong(1, "Morning raga"), testSong(2, "Morning song")),
            albums = listOf(testAlbum(1, "Morning")),
        )
    }
    private val playback = FakePlaybackController()
    private val queue = FakePlaybackQueue()

    private fun viewModel(repository: SearchRepository = search) =
        SearchViewModel(repository, playback, queue, FakeFavouriteRepository())

    @Test
    fun `nothing is searched before the person types`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(SearchUiState(), viewModel.state.value)
        assertTrue(search.searched.isEmpty())
    }

    @Test
    fun `the results are there within 300 ms of the last key`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.onTextChange("m")
        advanceTimeBy(100)
        viewModel.onTextChange("mo")
        advanceTimeBy(100)
        viewModel.onTextChange("mor")
        advanceTimeBy(199)
        assertTrue(search.searched.isEmpty())

        advanceTimeBy(101)

        assertEquals(listOf("mor"), search.searched)
        assertEquals("mor", viewModel.text)
        assertEquals(search.results, viewModel.state.value.results)
    }

    @Test
    fun `spaces around the text do not search again`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.onTextChange("mor")
        advanceUntilIdle()

        viewModel.onTextChange("mor ")
        advanceUntilIdle()

        assertEquals(listOf("mor"), search.searched)
        assertEquals("mor ", viewModel.text)
        assertEquals("mor", viewModel.state.value.searched)
    }

    @Test
    fun `an emptied field clears the results at once`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.onTextChange("mor")
        advanceUntilIdle()

        viewModel.onTextChange("")
        runCurrent()

        assertNull(viewModel.state.value.results)
        assertEquals(listOf("mor"), search.searched)
    }

    @Test
    fun `a text with no letter and no digit searches nothing`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.onTextChange(" - ")
        advanceUntilIdle()

        assertNull(viewModel.state.value.results)
        assertTrue(search.searched.isEmpty())
    }

    @Test
    fun `a search that fails says so, and the next text searches again`() = runTest(mainDispatcher.dispatcher) {
        var broken = true
        val failing = object : SearchRepository {
            override suspend fun search(text: String, limit: Int): SearchResults =
                if (broken) error("disk full") else search.search(text, limit)
        }
        val viewModel = viewModel(failing)

        viewModel.onTextChange("mor")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.failed)

        broken = false
        viewModel.onTextChange("morn")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.failed)
        assertEquals(search.results, viewModel.state.value.results)
    }

    @Test
    fun `a very long text is cut`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.onTextChange("a".repeat(1_000))
        advanceUntilIdle()

        assertEquals(256, viewModel.text.length)
    }

    @Test
    fun `a tap plays the song with the found songs as the queue`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.onTextChange("mor")
        advanceUntilIdle()

        viewModel.songActions.onPlay(search.results.songs[1])

        assertEquals(listOf("play [test:1, test:2] from 1"), playback.commands)
    }

    @Test
    fun `the menu puts a song next or at the end of the queue`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.songActions.onPlayNext(testSong(1))
        viewModel.songActions.onAddToQueue(testSong(2))

        assertEquals(listOf("playNext [test:1]", "addToEnd [test:2]"), queue.commands)
        assertTrue(playback.commands.isEmpty())
    }
}
