/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import com.siliconcircuits.tarangini.core.domain.library.ScanFailure
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryLetters
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryScanner
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
import com.siliconcircuits.tarangini.core.testing.library.testFolder
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import com.siliconcircuits.tarangini.core.testing.settings.FakeSettingsRepository
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val library = FakeLibraryRepository().apply {
        songs =
            listOf(
                testSong(1, "Cherry", durationMs = 300),
                testSong(2, "apple", durationMs = 100),
                testSong(3, "Banana", durationMs = 200),
            )
        albums = listOf(testAlbum(1, "Morning"))
        artists = listOf(testArtist(1, "Ravi"))
        folders = listOf(testFolder(1, "Music"), testFolder(2, "Ragas", parentId = 1))
    }
    private val settings = FakeSettingsRepository()
    private val scanner = FakeLibraryScanner()
    private val playback = FakePlaybackController()
    private val queue = FakePlaybackQueue()

    private fun viewModel() = LibraryViewModel(
        library,
        FakeLibraryLetters(library),
        settings,
        scanner,
        SongActions(playback, queue, FakeFavouriteRepository(), FakePlaylistRepository()),
    )

    private fun LibraryViewModel.titles() = songs.state.value.items.map { it.title }

    @Test
    fun `the lists are read when the screen opens`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("apple", "Banana", "Cherry"), viewModel.titles())
        assertFalse(viewModel.songs.state.value.isLoading)
        assertEquals(listOf("Morning"), viewModel.albums.state.value.items.map { it.title })
        assertEquals(listOf("Ravi"), viewModel.artists.state.value.items.map { it.name })
        assertEquals(listOf("Music"), viewModel.state.value.folders?.map { it.name })
    }

    @Test
    fun `a scan starts when the screen opens, and again on a tap`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(1, scanner.scanCount)

        viewModel.rescan()
        advanceUntilIdle()

        assertEquals(2, scanner.scanCount)
    }

    @Test
    fun `a scan that fails leaves the screen as it is`() = runTest(mainDispatcher.dispatcher) {
        scanner.nextOutcome = ScanOutcome.Failure(ScanFailure.entries.first())

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("apple", "Banana", "Cherry"), viewModel.titles())
    }

    @Test
    fun `the lists follow a change of the library`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        library.songs = library.songs + testSong(4, "Date")
        library.version.value++
        advanceUntilIdle()

        assertEquals(listOf("apple", "Banana", "Cherry", "Date"), viewModel.titles())
    }

    @Test
    fun `a chosen order is saved and the songs follow it`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setSort(SongSort.DURATION)
        advanceUntilIdle()

        assertEquals(SongSort.DURATION, settings.songSort().first())
        assertEquals(SongSort.DURATION, viewModel.state.value.sort)
        assertEquals(listOf("apple", "Banana", "Cherry"), viewModel.titles())
    }

    @Test
    fun `the saved order is used when the screen opens`() = runTest(mainDispatcher.dispatcher) {
        settings.setSongSort(SongSort.DATE_ADDED)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("Banana", "apple", "Cherry"), viewModel.titles())
    }

    @Test
    fun `a tap plays the song with the whole list as the queue`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.songActions.onPlay(library.songs.first { it.title == "Banana" })
        advanceUntilIdle()

        assertEquals(listOf("play [test:2, test:3, test:1] from 1"), playback.commands)
    }

    @Test
    fun `a long list is given to the player whole, in the order shown`() = runTest(mainDispatcher.dispatcher) {
        library.songs = List(1_200) { testSong(it.toLong(), title = "Song %04d".format(it)) }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.songActions.onPlay(library.songs[1_100])
        advanceUntilIdle()

        assertTrue(playback.commands.single().endsWith("from 1100"))
        assertEquals(100, viewModel.songs.state.value.items.size)
    }

    @Test
    fun `the menu puts a song next or at the end of the queue`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.songActions.onPlayNext(library.songs[0])
        viewModel.songActions.onAddToQueue(library.songs[1])

        assertEquals(listOf("playNext [test:1]", "addToEnd [test:2]"), queue.commands)
        assertNull(playback.commands.firstOrNull())
    }
}
