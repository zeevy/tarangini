/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import androidx.lifecycle.SavedStateHandle
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import com.siliconcircuits.tarangini.feature.library.album.AlbumViewModel
import com.siliconcircuits.tarangini.feature.library.artist.ArtistViewModel
import com.siliconcircuits.tarangini.feature.library.folder.FolderViewModel
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DetailViewModelsTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val library = FakeLibraryRepository().apply {
        songs = listOf(
            testSong(1, "One", artistId = 1, albumId = 10),
            testSong(2, "Two", artistId = 1, albumId = 11),
            testSong(3, "Three", artistId = 2, albumId = 12),
        )
        albums =
            listOf(
                testAlbum(10, "First", artistId = 1),
                testAlbum(11, "Second", artistId = 1),
                testAlbum(12, "Other", artistId = 2),
            )
        folders = listOf(testFolder(1, "Music"), testFolder(2, "Ragas", parentId = 1), testFolder(3, "Else"))
        folderOfSong = mapOf(1L to 1L, 2L to 1L, 3L to 2L)
    }
    private val playback = FakePlaybackController()
    private val actions = SongActions(playback, FakePlaybackQueue())

    @Test
    fun `an album shows its songs and plays them as the queue`() = runTest(mainDispatcher.dispatcher) {
        val saved = SavedStateHandle(mapOf("albumId" to 10L, "title" to "First", "artist" to "Ravi"))
        val viewModel = AlbumViewModel(saved, library, actions)
        advanceUntilIdle()

        assertEquals("First" to "Ravi", viewModel.title to viewModel.artist)
        assertEquals(listOf("One"), viewModel.songs.state.value.items.map { it.title })

        viewModel.songActions.onPlay(library.songs[0])
        advanceUntilIdle()
        assertEquals(listOf("play [test:1] from 0"), playback.commands)
    }

    @Test
    fun `an artist shows the albums and the songs, and plays the songs as the queue`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel =
                ArtistViewModel(SavedStateHandle(mapOf("artistId" to 1L, "name" to "Ravi")), library, actions)
            advanceUntilIdle()

            assertEquals("Ravi", viewModel.name)
            assertEquals(listOf("First", "Second"), viewModel.albums.value.map { it.title })
            assertEquals(listOf("One", "Two"), viewModel.songs.state.value.items.map { it.title })

            viewModel.songActions.onPlay(library.songs[1])
            advanceUntilIdle()
            assertEquals(listOf("play [test:1, test:2] from 1"), playback.commands)
        }

    @Test
    fun `a folder shows the folders inside it and its own songs`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = FolderViewModel(SavedStateHandle(mapOf("folderId" to 1L, "name" to "Music")), library, actions)
        advanceUntilIdle()

        assertEquals("Music", viewModel.name)
        assertEquals(listOf("Ragas"), viewModel.folders.value.map { it.name })
        assertEquals(listOf("One", "Two"), viewModel.songs.state.value.items.map { it.title })

        viewModel.songActions.onPlay(library.songs[1])
        advanceUntilIdle()
        assertEquals(listOf("play [test:1, test:2] from 1"), playback.commands)
    }

    @Test
    fun `a detail screen follows a change of the library`() = runTest(mainDispatcher.dispatcher) {
        val saved = SavedStateHandle(mapOf("albumId" to 10L, "title" to "First", "artist" to "Ravi"))
        val viewModel = AlbumViewModel(saved, library, actions)
        advanceUntilIdle()

        library.songs = library.songs + testSong(4, "Four", albumId = 10)
        library.version.value++
        advanceUntilIdle()

        assertEquals(listOf("Four", "One"), viewModel.songs.state.value.items.map { it.title })
    }
}
