/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
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

@RunWith(RobolectricTestRunner::class)
class QueueContentTest {
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
    private val actions = QueueActions(
        onPlay = { done += "play $it" },
        onMove = { from, to -> done += "move $from to $to" },
        onRemove = { done += "remove $it" },
    )
    private val songs = listOf(
        PlaybackItem("a", "First", "Ravi"),
        PlaybackItem("b", "Second", null),
        PlaybackItem("c", "Third", "Zakir"),
        // The same song a second time: it must show as its own row.
        PlaybackItem("a", "First", "Ravi"),
    )

    private fun show(state: QueueUiState) {
        composeRule.setContent {
            TaranginiTheme(dynamicColor = false) { QueueContent(state, actions, { done += "back" }) }
        }
    }

    /** Runs the action a screen reader offers under [label] on the row with [title]. */
    private fun rowAction(title: String, label: String) {
        val row = composeRule.onNode(hasText(title) and SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        val action = row.fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        composeRule.runOnUiThread { action.action() }
    }

    private fun actionLabels(title: String): List<String> =
        composeRule.onNode(hasText(title) and SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNode().config[SemanticsActions.CustomActions].map { it.label }

    @Test
    fun `the queue shows its items in order, and marks the one that plays`() {
        show(QueueUiState(songs, currentIndex = 1))

        composeRule.onNodeWithText("Third").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist").assertIsDisplayed()
        composeRule.onNode(hasText("Second") and SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .assert(
                SemanticsMatcher("plays now") {
                    it.config.getOrNull(SemanticsProperties.StateDescription) ==
                        "Playing now"
                },
            )
    }

    @Test
    fun `a tap on a row plays that item`() {
        show(QueueUiState(songs, currentIndex = 0))

        composeRule.onNodeWithText("Third").performClick()

        assertEquals(listOf("play 2"), done)
    }

    @Test
    fun `a swipe takes the row out of the queue`() {
        show(QueueUiState(songs, currentIndex = 0))

        // Over the whole row: a swipe over the title alone is too short to take the row out.
        composeRule.onNode(hasText("Second") and SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            // From the middle: the handle at the end of the row takes a touch for itself.
            .performTouchInput { swipeLeft(startX = centerX, endX = left) }
        composeRule.waitForIdle()

        assertEquals(listOf("remove 1"), done)
    }

    @Test
    fun `a row the player did not take out shows again`() {
        show(QueueUiState(songs, currentIndex = 0))

        composeRule.onNode(hasText("Second") and SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .performTouchInput { swipeLeft(startX = centerX, endX = left) }
        composeRule.mainClock.advanceTimeBy(1_000)

        composeRule.onNodeWithText("Second").assertIsDisplayed()
    }

    @Test
    fun `a screen reader can move a row up or down and remove it`() {
        show(QueueUiState(songs, currentIndex = 0))

        rowAction("Second", "Move up")
        rowAction("Second", "Move down")
        rowAction("Third", "Remove from the queue")

        assertEquals(listOf("move 1 to 0", "move 1 to 2", "remove 2"), done)
    }

    @Test
    fun `the first row cannot move up and the last one cannot move down`() {
        show(QueueUiState(songs.take(2), currentIndex = 0))

        assertEquals(listOf("Move down", "Remove from the queue"), actionLabels("First"))
        assertEquals(listOf("Move up", "Remove from the queue"), actionLabels("Second"))
    }

    @Test
    fun `an empty queue says so`() {
        show(QueueUiState())

        composeRule.onNodeWithText("The queue is empty").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("back"), done)
    }
}
