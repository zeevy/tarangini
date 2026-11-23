/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.genre

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeGenreRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.library.LibraryActions
import com.siliconcircuits.tarangini.feature.library.LibraryContent
import com.siliconcircuits.tarangini.feature.library.LibraryState
import com.siliconcircuits.tarangini.feature.library.LibraryTab
import com.siliconcircuits.tarangini.feature.library.checkAccessibility
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class GenreTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val rock = testSong(1, "Rock song")
    private val both = testSong(2, "Rock and pop song")
    private val library = FakeLibraryRepository().apply { songs = listOf(rock, both) }
    private val genres = FakeGenreRepository().apply {
        songsByGenre = linkedMapOf("Pop" to listOf(both), "Rock" to listOf(rock, both))
    }
    private val playback = FakePlaybackController()
    private val actions =
        SongActions(playback, FakePlaybackQueue(), FakeFavouriteRepository(), FakePlaylistRepository())

    private fun showList(onOpen: (Genre) -> Unit = {}) {
        val list = PagedItems(listOf(Genre(1, "Rock", 2), Genre(2, "", 1)), isLoading = false)
        val rowActions = SongRowActions(onPlay = {}, onPlayNext = {}, onAddToQueue = {})
        // Inside the frame of the Library tab, so the contrast is measured on the real background.
        composeRule.setContent {
            TaranginiTheme {
                LibraryContent(
                    LibraryState(),
                    rowActions,
                    LibraryActions(),
                    firstTab = LibraryTab.GENRES,
                    genres = { GenresContent(list, onOpen) },
                )
            }
        }
    }

    @Test
    fun `the list shows each genre with its songs, names the one with no name, and a tap opens it`() {
        val opened = mutableListOf<String>()
        showList { opened += it.name }

        composeRule.onNodeWithText("2 songs").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown").assertIsDisplayed()
        composeRule.onNodeWithText("Rock").performClick()

        assertEquals(listOf("Rock"), opened)
    }

    @Test
    fun `a genre with no name has a title, and its songs are listed`() {
        val songs = PagedItems(listOf(rock), isLoading = false)
        val rowActions = SongRowActions(onPlay = {}, onPlayNext = {}, onAddToQueue = {})
        composeRule.setContent { TaranginiTheme { GenreContent("", songs, rowActions, onBack = {}) } }

        composeRule.onNodeWithText("Unknown").assertIsDisplayed()
        composeRule.onNodeWithText("Rock song").assertIsDisplayed()
    }

    @Test
    fun `the list of genres is read, and again when the library changes`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = GenresViewModel(genres, library)
        advanceUntilIdle()

        assertEquals(
            listOf("Pop" to 1, "Rock" to 2),
            viewModel.genres.state.value.items.map {
                it.name to it.songCount
            },
        )
    }

    @Test
    fun `a genre shows its songs and plays them as the queue`() = runTest(mainDispatcher.dispatcher) {
        val saved = SavedStateHandle(mapOf("genreId" to 2L, "name" to "Rock"))
        val viewModel = GenreViewModel(saved, genres, library, actions)
        advanceUntilIdle()

        assertEquals("Rock", viewModel.name)
        assertEquals(listOf("Rock song", "Rock and pop song"), viewModel.songs.state.value.items.map { it.title })

        viewModel.songActions.onPlay(both)
        advanceUntilIdle()

        assertEquals(listOf("play [test:1, test:2] from 1 as GENRE Rock"), playback.commands)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the list passes the accessibility checks`() {
        showList()

        composeRule.checkAccessibility()
    }
}
