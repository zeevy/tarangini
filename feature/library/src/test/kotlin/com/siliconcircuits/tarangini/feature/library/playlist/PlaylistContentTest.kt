/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistImport
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.library.checkAccessibility
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
class PlaylistContentTest {
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
    private val places = PagedItems(
        listOf(PlaylistSong(7, testSong(1, "Bhairavi")), PlaylistSong(8, testSong(2, "Yaman"))),
        isLoading = false,
    )
    private val actions = PlaylistActions(
        onPlay = { done += "play ${it?.song?.title}" },
        onShuffle = { done += "shuffle" },
        onRename = { done += "rename $it" },
        onDelete = { done += "delete" },
        onRemove = { done += "remove ${it.itemId}" },
        onMove = { place, to -> done += "move ${place.itemId} $to" },
    )

    private fun show() = composeRule.setContent {
        TaranginiTheme { PlaylistContent("Drive", places, actions, onBack = {}) }
    }

    @Test
    fun `a tap plays from the song, and the top bar plays the whole playlist or mixes it`() {
        show()

        composeRule.onNodeWithText("Yaman").performClick()
        composeRule.onNodeWithContentDescription("Play the playlist").performClick()
        composeRule.onNodeWithContentDescription("Play in a mixed order").performClick()

        assertEquals(listOf("play Yaman", "play null", "shuffle"), done)
    }

    @Test
    fun `the menu of a place moves it or takes it out`() {
        show()

        composeRule.onNodeWithContentDescription("More options for Yaman").performClick()
        composeRule.onNodeWithText("Move to top").performClick()
        composeRule.onNodeWithContentDescription("More options for Bhairavi").performClick()
        composeRule.onNodeWithText("Remove from playlist").performClick()

        assertEquals(listOf("move 8 TOP", "remove 7"), done)
    }

    @Test
    fun `a playlist is removed only after the question is answered with Delete`() {
        show()

        composeRule.onNodeWithContentDescription("More options for the playlist").performClick()
        composeRule.onNodeWithText("Delete playlist").performClick()
        composeRule.onNodeWithText("Delete the playlist Drive? Its songs stay in your library.").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        val afterCancel = done.toList()
        composeRule.onNodeWithContentDescription("More options for the playlist").performClick()
        composeRule.onNodeWithText("Delete playlist").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(emptyList<String>(), afterCancel)
        assertEquals(listOf("delete"), done)
    }

    @Test
    fun `a new playlist gets the typed name`() {
        composeRule.setContent {
            TaranginiTheme {
                PlaylistsContent(listOf(Playlist(1, "Drive", 2)), onOpenPlaylist = { done += "open ${it.name}" }, {
                    done += "create $it"
                })
            }
        }

        composeRule.onNodeWithText("Drive").performClick()
        composeRule.onNodeWithText("New playlist").performClick()
        composeRule.onNodeWithText("Name").performTextInput("Morning")
        composeRule.onNodeWithText("Create").performClick()

        assertEquals(listOf("open Drive", "create Morning"), done)
    }

    @Test
    fun `the list offers the import of a file and says how it ended`() {
        val result = PlaylistImport.Done(1, "Evening", matched = 1, total = 2, notFound = listOf("Rock/gone.mp3"))
        var shown by mutableStateOf<PlaylistImport?>(null)
        composeRule.setContent {
            TaranginiTheme {
                PlaylistsContent(
                    playlists = emptyList(),
                    onOpenPlaylist = {},
                    onCreate = {},
                    files = PlaylistFileActions(
                        imported = shown,
                        onImport = { shown = result },
                        onClose = { shown = null },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Import a playlist file").performClick()

        composeRule.onNodeWithText("1 of 2 songs were found in your library.").assertIsDisplayed()
        composeRule.onNodeWithText("Rock/gone.mp3").assertIsDisplayed()

        composeRule.onNodeWithText("OK").performClick()

        composeRule.onNodeWithText("Rock/gone.mp3").assertDoesNotExist()
    }

    @Test
    fun `a file that is refused says why`() {
        composeRule.setContent {
            TaranginiTheme {
                PlaylistsContent(emptyList(), {}, {}, files = PlaylistFileActions(imported = PlaylistImport.TooLarge))
            }
        }

        composeRule.onNodeWithText("This file is too large to be a playlist file.").assertIsDisplayed()
    }

    @Test
    fun `the menu of a playlist exports it, and the result is told`() {
        var asked = 0
        composeRule.setContent {
            TaranginiTheme {
                PlaylistContent(
                    "Drive",
                    places,
                    actions,
                    onBack = {},
                    files = PlaylistFileActions(exported = true.takeIf { asked > 0 }, onExport = { asked++ }),
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options for the playlist").performClick()
        composeRule.onNodeWithText("Export to a file").performClick()

        assertEquals(1, asked)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the screen passes the accessibility checks`() {
        show()

        composeRule.checkAccessibility()
    }
}
