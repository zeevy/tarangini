/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
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

@RunWith(RobolectricTestRunner::class)
class MiniPlayerBarTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val done = mutableListOf<String>()
    private val playing =
        MiniPlayerUiState("Bhairavi", "Ravi", null, showPause = true, isBuffering = false, progress = 0.4f)

    private fun show(state: MiniPlayerUiState) {
        composeRule.setContent {
            TaranginiTheme {
                MiniPlayerBar(state, { done += "toggle" }, { done += "next" }, { done += "open" })
            }
        }
    }

    @Test
    fun `the bar shows the item, and each part does its own thing`() {
        show(playing)

        composeRule.onNodeWithText("Bhairavi").assertIsDisplayed()
        composeRule.onNodeWithText("Ravi").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next").performClick()
        composeRule.onNodeWithText("Bhairavi").performClick()

        assertEquals(listOf("toggle", "next", "open"), done)
    }

    @Test
    fun `a paused item offers play`() {
        show(playing.copy(showPause = false))

        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }

    @Test
    fun `a station with no song title shows its name and that it is live`() {
        show(playing.copy(title = "Radio Mirchi", artist = null, progress = null, isStream = true))

        composeRule.onNodeWithText("Radio Mirchi").assertIsDisplayed()
        composeRule.onNodeWithText("Live").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist").assertDoesNotExist()
    }

    @Test
    fun `an item with no texts says so`() {
        show(playing.copy(title = null, artist = null))

        composeRule.onNodeWithText("Unknown title").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist").assertIsDisplayed()
    }

    @Test
    fun `a stream that waits for data says that it loads, and can still be paused`() {
        // The line of this state moves for ever, so the test does not wait for it to stop.
        composeRule.mainClock.autoAdvance = false
        show(playing.copy(isBuffering = true, progress = null))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").performClick()

        assertEquals(listOf("toggle"), done)
    }

    @Test
    fun `each button of the bar can be touched on 48 dp`() {
        show(playing)

        // Measured here and not with the framework check: on the JVM it reads the buttons as 46 dp high.
        listOf("Pause", "Next").forEach { button ->
            val touch = composeRule.onNodeWithContentDescription(button).fetchSemanticsNode().touchBoundsInRoot
            with(composeRule.density) {
                assertTrue("$button is ${touch.width} x ${touch.height} px", touch.width >= 48.dp.toPx())
                assertTrue("$button is ${touch.width} x ${touch.height} px", touch.height >= 48.dp.toPx())
            }
        }
    }
}
