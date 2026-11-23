/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSongLookup
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val queue = FakePlaybackQueue()
    private val songs = listOf(PlaybackItem("a", "First"), PlaybackItem("b", "Second"), PlaybackItem("c", "Third"))
    private val playlists = FakePlaylistRepository()
    private val lookup = FakeSongLookup()
    private val library = FakeLibraryRepository()

    private fun viewModel() = QueueViewModel(playback, queue, playlists, lookup, library)

    @Test
    fun `the queue shows with the place of the item that plays, also when it was loaded before`() =
        runTest(mainDispatcher.dispatcher) {
            playback.state.value = PlaybackState(queue = songs, currentIndex = 1)

            val viewModel = viewModel()

            assertEquals(QueueUiState(songs, 1), viewModel.state.value)
        }

    @Test
    fun `a new place of the player does not give a new list, a new current item does`() =
        runTest(mainDispatcher.dispatcher) {
            playback.state.value = PlaybackState(queue = songs, currentIndex = 1)
            val viewModel = viewModel()
            val seen = mutableListOf<QueueUiState>()
            backgroundScope.launch { viewModel.state.collect { seen += it } }
            runCurrent()

            playback.state.value = playback.state.value.copy(positionMs = 5_000)
            runCurrent()
            playback.state.value = playback.state.value.copy(currentIndex = 2)
            runCurrent()

            assertEquals(listOf(1, 2), seen.map { it.currentIndex })
            assertSame(songs, seen.last().items)
        }

    @Test
    fun `play and move reach the queue with their places`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.playAt(2)
        viewModel.move(from = 2, to = 0)

        assertEquals(listOf("playAt 2", "move 2 to 0"), queue.commands)
    }

    @Test
    fun `a removed song waits for Undo, then leaves the queue`() = runTest(mainDispatcher.dispatcher) {
        playback.state.value = PlaybackState(queue = songs, currentIndex = 0)
        val viewModel = viewModel()

        viewModel.remove(2)
        assertEquals(PendingRemoval(2, "c", "Third"), viewModel.pendingRemoval.value)
        assertEquals(emptyList<String>(), queue.commands)

        advanceTimeBy(4_001)
        assertEquals(listOf("remove 2"), queue.commands)
        assertNull(viewModel.pendingRemoval.value)
    }

    @Test
    fun `Undo keeps the song in the queue`() = runTest(mainDispatcher.dispatcher) {
        playback.state.value = PlaybackState(queue = songs, currentIndex = 0)
        val viewModel = viewModel()

        viewModel.remove(1)
        viewModel.undoRemove()
        advanceTimeBy(10_000)

        assertEquals(emptyList<String>(), queue.commands)
        assertNull(viewModel.pendingRemoval.value)
    }

    @Test
    fun `a second remove first takes out the one before, found again after the queue changed`() =
        runTest(mainDispatcher.dispatcher) {
            playback.state.value = PlaybackState(queue = songs, currentIndex = 0)
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.state.collect() }
            runCurrent()

            viewModel.remove(2)
            // The song moved to place 1 while Undo was offered.
            playback.state.value = PlaybackState(queue = listOf(songs[0], songs[2], songs[1]), currentIndex = 0)
            runCurrent()
            viewModel.remove(2)

            assertEquals(listOf("remove 1"), queue.commands)
            assertEquals("b", viewModel.pendingRemoval.value?.itemId)
        }

    @Test
    fun `clear keeps only the song that plays`() = runTest(mainDispatcher.dispatcher) {
        viewModel().clear()

        assertEquals(listOf("keepOnlyCurrent"), queue.commands)
    }

    @Test
    fun `the queue is saved in its order as a new playlist, and the screen is told`() =
        runTest(mainDispatcher.dispatcher) {
            val known = listOf(testSong(1), testSong(2))
            lookup.songs = known
            playback.state.value = PlaybackState(
                queue = listOf(PlaybackItem("test:2"), PlaybackItem("gone"), PlaybackItem("test:1")),
                currentIndex = 0,
            )
            val viewModel = viewModel()
            val notices = mutableListOf<QueueNotice>()
            backgroundScope.launch { viewModel.notice.collect { notices += it } }
            runCurrent()

            viewModel.saveAsPlaylist("  Drive  ")
            runCurrent()

            val id = playlists.names.entries.single { it.value == "Drive" }.key
            assertEquals(listOf(2L, 1L), playlists.places[id]?.map { it.second })
            assertEquals(listOf<QueueNotice>(QueueNotice.Saved("Drive")), notices)
        }

    @Test
    fun `a queue with no song of the library is not saved`() = runTest(mainDispatcher.dispatcher) {
        playback.state.value = PlaybackState(queue = listOf(PlaybackItem("gone")), currentIndex = 0)
        val viewModel = viewModel()
        val notices = mutableListOf<QueueNotice>()
        backgroundScope.launch { viewModel.notice.collect { notices += it } }
        runCurrent()

        viewModel.saveAsPlaylist("Drive")
        runCurrent()

        assertTrue(playlists.names.isEmpty())
        assertEquals(listOf<QueueNotice>(QueueNotice.NotSaved), notices)
    }

    @Test
    fun `shuffle all plays every song of the library`() = runTest(mainDispatcher.dispatcher) {
        library.songs = (1L..3L).map { testSong(it) }

        viewModel().shuffleAll()
        runCurrent()

        val command = playback.commands.single()
        assertTrue(command, command.startsWith("play ") && command.endsWith(" from 0"))
        listOf("test:1", "test:2", "test:3").forEach { assertTrue(command, it in command) }
    }
}
