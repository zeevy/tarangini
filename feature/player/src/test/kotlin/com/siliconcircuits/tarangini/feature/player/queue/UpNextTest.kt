/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.checkAccessibility
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class UpNextTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivityRule()).around(composeRule)

    private val done = mutableListOf<String>()
    private val actions = QueueActions(
        onPlay = { done += "play $it" },
        onMove = { from, to -> done += "move $from to $to" },
        onRemove = { done += "remove $it" },
        onSave = { done += "save" },
        onClear = { done += "clear" },
    )
    private val items = listOf(
        PlaybackItem("a", "Morning raga", "Ravi Shankar", durationMs = 200_000),
        PlaybackItem("b", "Evening raga", "Zakir Hussain", durationMs = 185_000),
        PlaybackItem("c", "Night raga", "Hariprasad", durationMs = 190_000),
        PlaybackItem("d", "Dawn raga", null, durationMs = 180_000),
    )

    private fun show(state: QueueUiState, hidden: Int? = null) {
        composeRule.setContent {
            TaranginiTheme { UpNextCard(state, actions, rememberLazyListState(), hidden = hidden) }
        }
    }

    private fun row(place: Int, title: String, artist: String?, length: String, playing: Boolean = false) =
        composeRule.onNodeWithContentDescription(
            listOfNotNull("Number $place", title, artist, length, "Playing now".takeIf { playing }).joinToString(", "),
        )

    @Test
    fun `the list starts at the song that plays and says what is left`() {
        show(QueueUiState(items, currentIndex = 1))

        composeRule.onNodeWithText("2 left · 6:10").assertIsDisplayed()
        row(2, "Evening raga", "Zakir Hussain", "3:05", playing = true).assertIsDisplayed()
        row(3, "Night raga", "Hariprasad", "3:10").assertIsDisplayed()
        row(4, "Dawn raga", null, "3:00").assertIsDisplayed()
        composeRule.onNodeWithText("Morning raga", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a tap on a row plays it, and the tools reach the queue`() {
        show(QueueUiState(items, currentIndex = 0))

        row(3, "Night raga", "Hariprasad", "3:10").performClick()
        composeRule.onNodeWithContentDescription("Save as a playlist").performClick()
        composeRule.onNodeWithContentDescription("Clear the queue").performClick()

        assertEquals(listOf("play 2", "save", "clear"), done)
    }

    @Test
    fun `TalkBack can move and remove a row`() {
        show(QueueUiState(items, currentIndex = 0))

        val actionsOf = { node: SemanticsNodeInteraction ->
            node.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        }
        val first = actionsOf(row(2, "Evening raga", "Zakir Hussain", "3:05"))
        assertEquals(listOf("Move down", "Remove from the queue"), first.map { it.label })
        val middle = actionsOf(row(3, "Night raga", "Hariprasad", "3:10"))
        assertEquals(listOf("Move up", "Move down", "Remove from the queue"), middle.map { it.label })
        composeRule.runOnUiThread {
            middle[0].action()
            middle[2].action()
        }

        assertEquals(listOf("move 2 to 1", "remove 2"), done)
    }

    @Test
    fun `a swipe to the left takes a song out`() {
        show(QueueUiState(items, currentIndex = 0))

        row(4, "Dawn raga", null, "3:00").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(listOf("remove 3"), done)
    }

    @Test
    fun `a song waiting for Undo is not shown`() {
        show(QueueUiState(items, currentIndex = 0), hidden = 2)

        composeRule.onNodeWithText("2 left · 6:05").assertIsDisplayed()
        composeRule.onNodeWithText("Night raga", substring = true).assertDoesNotExist()
    }

    @Test
    fun `with shuffle on the rows follow the order of the player and cannot be moved`() {
        show(QueueUiState(items, currentIndex = 0, playOrder = listOf(0, 3, 1)))

        composeRule.onNodeWithText("2 left · 6:05").assertIsDisplayed()
        val labels = row(4, "Dawn raga", null, "3:00").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Remove from the queue"), labels.map { it.label })
    }

    @Test
    fun `an empty queue says so and has no tools`() {
        show(QueueUiState())

        composeRule.onNodeWithText("Queue empty").assertIsDisplayed()
        composeRule.onNodeWithText("The queue is empty").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear the queue").assertDoesNotExist()
    }

    @Test
    fun `a stream has no tools`() {
        show(QueueUiState(listOf(PlaybackItem("http://radio", "Radio", isStream = true)), currentIndex = 0))

        composeRule.onNodeWithContentDescription("Save as a playlist").assertDoesNotExist()
    }

    @Test
    fun `up next passes the accessibility checks`() {
        show(QueueUiState(items, currentIndex = 1))

        composeRule.checkAccessibility()
    }
}
