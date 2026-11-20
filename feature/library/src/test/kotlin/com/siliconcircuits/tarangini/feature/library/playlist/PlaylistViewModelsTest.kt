/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import androidx.lifecycle.SavedStateHandle
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.feature.library.MainDispatcherRule
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaylistViewModelsTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val songs = listOf(testSong(1, "One"), testSong(2, "Two"), testSong(3, "Three"))
    private val library = FakeLibraryRepository().apply { songs = this@PlaylistViewModelsTest.songs }
    private val playlists = FakePlaylistRepository().apply { known = songs }
    private val playback = FakePlaybackController()

    private suspend fun TestScope.filled(): PlaylistViewModel {
        val id = requireNotNull(playlists.create("Drive"))
        songs.forEach { playlists.add(id, it.id) }
        val saved = SavedStateHandle(mapOf("playlistId" to id, "name" to "Drive"))
        return PlaylistViewModel(saved, playlists, library, playback).also { advanceUntilIdle() }
    }

    private fun PlaylistViewModel.titles() = songs.state.value.items.map { it.song.title }

    @Test
    fun `the list of playlists shows a new playlist, and a bad name makes none`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = PlaylistsViewModel(playlists, library)

        viewModel.create("Morning")
        viewModel.create("   ")
        advanceUntilIdle()

        assertEquals(listOf("Morning"), viewModel.state.value.items.map { it.name })
    }

    @Test
    fun `a playlist shows its songs in order and plays from the tapped song`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = filled()

        viewModel.play(viewModel.songs.state.value.items[1])
        viewModel.play()
        advanceUntilIdle()

        assertEquals(listOf("One", "Two", "Three"), viewModel.titles())
        assertEquals(
            listOf("play [test:1, test:2, test:3] from 1", "play [test:1, test:2, test:3] from 0"),
            playback.commands,
        )
    }

    @Test
    fun `move down does nothing at the end of the rows read so far when the playlist goes on`() =
        runTest(mainDispatcher.dispatcher) {
            val many = (1L..150L).map { testSong(it, "Song $it") }
            playlists.known = many
            val id = requireNotNull(playlists.create("Long"))
            many.forEach { playlists.add(id, it.id) }
            val saved = SavedStateHandle(mapOf("playlistId" to id, "name" to "Long"))
            val viewModel = PlaylistViewModel(saved, playlists, library, playback).also { advanceUntilIdle() }
            val rows = viewModel.songs.state.value.items
            val before = playlists.places.getValue(id)

            viewModel.move(rows[rows.lastIndex - 1], PlaylistMove.DOWN)
            advanceUntilIdle()

            assertTrue(rows.size < many.size)
            assertEquals(before, playlists.places.getValue(id))
        }

    @Test
    fun `shuffle plays every song of the playlist`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = filled()

        viewModel.shuffle()
        advanceUntilIdle()

        val command = playback.commands.single()
        assertTrue(command, listOf("test:1", "test:2", "test:3").all { it in command } && command.endsWith("from 0"))
    }

    @Test
    fun `a place moves up, down, to the top and to the end, and can be taken out`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = filled()
            val seen = mutableListOf<List<String>>()
            suspend fun act(block: (PlaylistViewModel) -> Unit) {
                block(viewModel)
                advanceUntilIdle()
                seen += viewModel.titles()
            }
            fun place(title: String) = viewModel.songs.state.value.items.first { it.song.title == title }

            act { it.move(place("Three"), PlaylistMove.UP) }
            act { it.move(place("One"), PlaylistMove.DOWN) }
            act { it.move(place("Two"), PlaylistMove.TOP) }
            act { it.move(place("Two"), PlaylistMove.END) }
            act { it.move(place("Two"), PlaylistMove.DOWN) }
            act { it.move(place("Three"), PlaylistMove.UP) }
            act { it.remove(place("One")) }

            assertEquals(
                listOf(
                    listOf("One", "Three", "Two"),
                    listOf("Three", "One", "Two"),
                    listOf("Two", "Three", "One"),
                    listOf("Three", "One", "Two"),
                    listOf("Three", "One", "Two"),
                    listOf("Three", "One", "Two"),
                    listOf("Three", "Two"),
                ),
                seen,
            )
        }

    @Test
    fun `a rename shows the new name, and a removed playlist closes the screen`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = filled()

        viewModel.rename("Night")
        advanceUntilIdle()
        val renamed = viewModel.state.value
        viewModel.delete()
        advanceUntilIdle()

        assertEquals(PlaylistUiState("Night"), renamed)
        assertTrue(viewModel.state.value.isGone)
    }
}
