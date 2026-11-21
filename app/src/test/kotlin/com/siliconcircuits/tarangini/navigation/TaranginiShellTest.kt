/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.testing.NoPlaybackServiceRule
import com.siliconcircuits.tarangini.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.testing.checkAccessibility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class TaranginiShellTest {
    @get:Rule(order = 0)
    val registerActivity = RegisterComponentActivityRule()

    @get:Rule(order = 0)
    val noPlaybackService = NoPlaybackServiceRule()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun showShell() {
        composeRule.setContent {
            TaranginiTheme { TaranginiShell() }
        }
    }

    @Test
    fun `the app opens to Home`() {
        composeRule.onNode(tab("Home")).assertIsSelected()
        composeRule.onNode(title("Tarangini")).assertIsDisplayed()
    }

    @Test
    fun `each tab opens its own screen`() {
        // The Radio tab needs Hilt, which this plain activity does not have. Its content is tested in the feature.
        listOf("Library", "Favourites").forEach { name ->
            composeRule.onNode(tab(name)).performClick()

            composeRule.onNode(tab(name)).assertIsSelected()
            composeRule.onNode(title(name)).assertIsDisplayed()
        }

        composeRule.onNode(tab("Home")).performClick()

        composeRule.onNode(title("Tarangini")).assertIsDisplayed()
    }

    @Test
    fun `Settings opens from Home without the bottom bar and Back returns to Home`() {
        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.onNode(title("Settings")).assertIsDisplayed()
        composeRule.onNode(tab("Home")).assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNode(tab("Home")).assertIsSelected()
        composeRule.onNode(title("Tarangini")).assertIsDisplayed()
    }

    @Test
    fun `system Back on Settings returns to Home and keeps the app open`() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNode(title("Settings")).assertIsDisplayed()

        pressBack()

        composeRule.onNode(tab("Home")).assertIsSelected()
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun `Back from the first screen of a tab leaves the app`() {
        composeRule.onNode(tab("Library")).performClick()
        composeRule.onNode(title("Library")).assertIsDisplayed()

        pressBack()

        assertTrue(composeRule.activity.isFinishing)
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    // A tab and the title of its screen can carry the same text. Only the tab can be selected.
    private fun tab(label: String): SemanticsMatcher = hasText(label) and isSelectable()

    private fun title(text: String): SemanticsMatcher = hasText(text) and isSelectable().not()

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the shell passes the accessibility checks`() {
        composeRule.checkAccessibility()
    }
}
