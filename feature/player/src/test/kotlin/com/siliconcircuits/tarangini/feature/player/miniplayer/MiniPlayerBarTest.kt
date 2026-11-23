/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.checkAccessibility
import com.siliconcircuits.tarangini.feature.player.nowplaying.CompactSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class MiniPlayerBarTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val done = mutableListOf<String>()
    private val playing = CompactSong("Bhairavi", "Ravi", 65_000, 185_000, showPause = true, isBuffering = false)

    private fun show(state: CompactSong) {
        composeRule.setContent {
            TaranginiTheme {
                MiniPlayerBar(state, { done += "toggle" }, { done += "next" }, { done += "open" })
            }
        }
    }

    @Test
    fun `the card shows the item and the time, and each part does its own thing`() {
        show(playing)

        composeRule.onNodeWithContentDescription("Bhairavi, Ravi. Open the player").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Next").performClick()
        composeRule.onNodeWithContentDescription("Bhairavi, Ravi. Open the player").performClick()

        assertEquals(listOf("toggle", "next", "open"), done)
    }

    @Test
    fun `a paused item offers play`() {
        show(playing.copy(showPause = false))

        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }

    @Test
    fun `an item with no texts says so`() {
        show(playing.copy(title = null, artist = null))

        composeRule.onNodeWithContentDescription("Unknown title, Unknown artist. Open the player").assertIsDisplayed()
    }

    @Test
    fun `a station with no song artist says that it is live`() {
        show(CompactSong("Radio Mirchi", null, 0, null, showPause = true, isBuffering = false, isStream = true))

        composeRule.onNodeWithContentDescription("Radio Mirchi, Live. Open the player").assertIsDisplayed()
    }

    @Test
    fun `each key of the card can be touched on 48 dp`() {
        show(playing)

        listOf("Pause", "Next").forEach { key ->
            val touch = composeRule.onNodeWithContentDescription(key).fetchSemanticsNode().touchBoundsInRoot
            with(composeRule.density) {
                assertTrue("$key is ${touch.width} x ${touch.height} px", touch.width >= 48.dp.toPx())
                assertTrue("$key is ${touch.width} x ${touch.height} px", touch.height >= 48.dp.toPx())
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the card passes the accessibility checks`() {
        show(playing)

        composeRule.checkAccessibility()
    }

    @Test
    @Config(qualifiers = "+night")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the card passes the accessibility checks in the dark look`() {
        show(playing)

        composeRule.checkAccessibility()
    }
}
