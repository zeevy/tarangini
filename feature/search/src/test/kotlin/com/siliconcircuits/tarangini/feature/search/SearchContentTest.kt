/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.search

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class SearchContentTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val done = mutableListOf<String>()
    private val songActions = SongRowActions(
        onPlay = { done += "play ${it.title}" },
        onPlayNext = { done += "next ${it.title}" },
        onAddToQueue = { done += "queue ${it.title}" },
    )
    private val results = SearchResults(
        songs = listOf(testSong(1, "Morning raga", artist = "Ravi")),
        albums = listOf(testAlbum(1, "Morning", artist = "Ravi", songCount = 3)),
        artists = listOf(testArtist(1, "Morning Star", songCount = 5)),
    )

    /** Counts how often the screen asked to close the keyboard. */
    private val keyboard = object : SoftwareKeyboardController {
        var hidden = 0

        override fun show() = Unit

        override fun hide() {
            hidden++
        }
    }

    private var openedAlbum: Album? = null
    private var openedArtist: Artist? = null
    private var typed by mutableStateOf("")

    private fun show(state: SearchUiState) {
        typed = state.searched
        composeRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                TaranginiTheme {
                    SearchContent(
                        text = typed,
                        state = state,
                        songActions = songActions,
                        onTextChange = { typed = it },
                        onOpenAlbum = { openedAlbum = it },
                        onOpenArtist = { openedArtist = it },
                        onBack = { done += "back" },
                    )
                }
            }
        }
    }

    @Test
    fun `the field has the cursor and takes what is typed`() {
        show(SearchUiState())

        composeRule.onNodeWithText("Search your music").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        composeRule.onNode(hasSetTextAction()).performTextInput("mor")

        assertEquals("mor", typed)
    }

    @Test
    fun `a screen that comes back with a text does not take the cursor again`() {
        show(SearchUiState("mor", results))

        composeRule.onNode(hasSetTextAction()).assertIsNotFocused()
    }

    @Test
    fun `the results show in three groups, and a tap plays or opens`() {
        show(SearchUiState("mor", results))

        composeRule.onNodeWithText("Songs").assertIsDisplayed()
        composeRule.onNodeWithText("Albums").assertIsDisplayed()
        composeRule.onNodeWithText("Artists").assertIsDisplayed()

        composeRule.onNodeWithText("Morning raga").performClick()
        composeRule.onNodeWithText("Morning").performClick()
        composeRule.onNodeWithText("Morning Star").performClick()

        assertEquals(listOf("play Morning raga"), done)
        assertEquals("Morning", openedAlbum?.title)
        assertEquals("Morning Star", openedArtist?.name)
    }

    @Test
    fun `a group with nothing in it has no title`() {
        show(SearchUiState("mor", SearchResults(albums = results.albums)))

        composeRule.onNodeWithText("Albums").assertIsDisplayed()
        composeRule.onNodeWithText("Songs").assertDoesNotExist()
        composeRule.onNodeWithText("Artists").assertDoesNotExist()
    }

    @Test
    fun `the clear button empties the field`() {
        show(SearchUiState("mor", results))

        composeRule.onNodeWithContentDescription("Clear the search text").performClick()

        assertEquals("", typed)
        composeRule.onNodeWithContentDescription("Clear the search text").assertDoesNotExist()
    }

    @Test
    fun `no result names the text that found nothing`() {
        show(SearchUiState("xyz", SearchResults()))

        composeRule.onNodeWithText("Nothing found").assertIsDisplayed()
        composeRule.onNodeWithText("No song, album or artist matches \"xyz\".").assertIsDisplayed()
    }

    @Test
    fun `a search that failed says so`() {
        show(SearchUiState("mor", failed = true))

        composeRule.onNodeWithText("The search did not work").assertIsDisplayed()
    }

    @Test
    fun `the keyboard closes when the results are scrolled`() {
        val many = SearchResults(songs = List(40) { testSong(it.toLong(), "Morning $it") })
        show(SearchUiState("mor", many))
        composeRule.waitForIdle()
        val before = keyboard.hidden

        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertTrue(keyboard.hidden > before)
    }

    @Test
    fun `back leaves the screen`() {
        show(SearchUiState())

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("back"), done)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the screen passes the accessibility checks`() {
        show(SearchUiState("mor", results))

        composeRule.checkAccessibility()
    }

    @Test
    @Config(qualifiers = "+night")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the screen passes the accessibility checks in the dark look`() {
        show(SearchUiState("mor", results))

        composeRule.checkAccessibility()
    }
}
