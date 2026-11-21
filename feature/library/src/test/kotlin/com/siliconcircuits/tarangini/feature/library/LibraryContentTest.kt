/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
import com.siliconcircuits.tarangini.core.testing.library.testFolder
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
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
class LibraryContentTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** The manifest of a library has no activity, so the plain one is added before Compose starts it. */
    private val registerActivity = object : TestWatcher() {
        override fun starting(description: Description) {
            shadowOf(app.packageManager)
                .addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val done = mutableListOf<String>()
    private val songActions = SongRowActions(
        onPlay = { done += "play ${it.title}" },
        onPlayNext = { done += "next ${it.title}" },
        onAddToQueue = { done += "queue ${it.title}" },
    )
    private val lists = LibraryState(
        songs = PagedItems(listOf(testSong(1, "Bhairavi", artist = "Ravi"), testSong(2, "Yaman", artist = "")), false),
        albums = PagedItems(listOf(testAlbum(1, "Morning", artist = "Ravi", songCount = 1)), false),
        artists = PagedItems(listOf(testArtist(1, "Ravi")), false),
        folders = listOf(testFolder(1, "Music")),
    )

    private fun show(lists: LibraryState = this.lists, actions: LibraryActions = LibraryActions()) {
        composeRule.setContent {
            TaranginiTheme { LibraryContent(lists, songActions, actions) }
        }
    }

    @Test
    fun `a row says the title, the artist and the length, and a tap plays the song`() {
        show()

        composeRule.onNodeWithText("Ravi · 3:05").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist · 3:05").assertIsDisplayed()
        composeRule.onNodeWithText("Bhairavi").performClick()

        assertEquals(listOf("play Bhairavi"), done)
    }

    @Test
    fun `the menu of a row is named after its song and puts the song in the queue`() {
        show()

        composeRule.onNodeWithContentDescription("More options for Yaman").performClick()
        composeRule.onNodeWithText("Play next").performClick()
        composeRule.onNodeWithContentDescription("More options for Bhairavi").performClick()
        composeRule.onNodeWithText("Add to queue").performClick()

        assertEquals(listOf("next Yaman", "queue Bhairavi"), done)
    }

    @Test
    fun `the sort menu marks the order that is on and tells the choice`() {
        var chosen: SongSort? = null
        show(lists.copy(sort = SongSort.ARTIST), LibraryActions(onSort = { chosen = it }))

        composeRule.onNodeWithContentDescription("Sort songs").performClick()
        composeRule.onNodeWithText("Shortest first").performClick()

        assertEquals(SongSort.DURATION, chosen)
    }

    @Test
    fun `each tab shows its list, and a row opens what it shows`() {
        var opened: Album? = null
        show(actions = LibraryActions(onOpenAlbum = { opened = it }))

        composeRule.onNodeWithText("Albums").performClick()
        composeRule.onNodeWithText("Albums").assertIsSelected()
        composeRule.onNodeWithText("Ravi · 1 song").assertIsDisplayed()
        composeRule.onNodeWithText("Morning").performClick()
        assertEquals("Morning", opened?.title)

        composeRule.onNodeWithText("Artists").performClick()
        composeRule.onNodeWithText("12 songs").assertIsDisplayed()

        composeRule.onNodeWithText("Folders").performClick()
        composeRule.onNodeWithText("Music").assertIsDisplayed()
    }

    @Test
    fun `an empty library says so and offers to look again`() {
        var rescans = 0
        show(
            LibraryState(songs = PagedItems(emptyList<Song>(), isLoading = false), folders = emptyList()),
            LibraryActions(onRescan = { rescans++ }),
        )

        composeRule.onNodeWithText("No music found").assertIsDisplayed()
        composeRule.onNodeWithText("Look again").performClick()

        assertEquals(1, rescans)
    }

    @Test
    fun `a list that could not be read says so and offers to try again`() {
        var retries = 0
        show(
            LibraryState(songs = PagedItems(emptyList<Song>(), isLoading = false, failed = true)),
            LibraryActions(onRetry = { retries++ }),
        )

        composeRule.onNodeWithText("The library could not be read").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `a list with more rows asks for them when its end is on screen`() {
        var asked = 0
        val few = PagedItems(listOf(testSong(1), testSong(2)), isLoading = false, hasMore = true)

        show(lists.copy(songs = few), LibraryActions(onMoreSongs = { asked++ }))
        composeRule.waitForIdle()

        assertEquals(1, asked)
    }

    @Test
    fun `the search button opens the search`() {
        var searches = 0
        show(actions = LibraryActions(onSearch = { searches++ }))

        composeRule.onNodeWithContentDescription("Search").performClick()

        assertEquals(1, searches)
    }

    @Test
    fun `a list that is not read yet shows that it loads`() {
        show(LibraryState())

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the screen passes the accessibility checks`() {
        show()

        composeRule.checkAccessibility()
    }
}
