/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.testing.NoPlaybackServiceRule
import com.siliconcircuits.tarangini.testing.RegisterComponentActivityRule
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Where the shell puts the mini player. A plain text stands in for the bar itself. */
@RunWith(RobolectricTestRunner::class)
class TaranginiShellMiniPlayerTest {
    @get:Rule(order = 0)
    val registerActivity = RegisterComponentActivityRule()

    @get:Rule(order = 0)
    val noPlaybackService = NoPlaybackServiceRule()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun showShell() {
        composeRule.setContent {
            TaranginiTheme {
                TaranginiShell(miniPlayer = { onOpen, modifier ->
                    Text("mini player", modifier.testTag("mini").clickable(onClick = onOpen))
                })
            }
        }
    }

    @Test
    fun `the mini player shows on every tab, above the bottom bar`() {
        // The Radio tab needs Hilt, which this plain activity does not have.
        listOf("Home", "Library", "Favourites").forEach { name ->
            composeRule.onNode(hasText(name) and isSelectable()).performClick()

            val mini = composeRule.onNodeWithTag("mini").assertIsDisplayed().getUnclippedBoundsInRoot()
            val tab = composeRule.onNode(hasText(name) and isSelectable()).getUnclippedBoundsInRoot()
            assertTrue("The mini player is not above the bottom bar on $name", mini.bottom <= tab.top)
        }
    }

    @Test
    fun `the mini player stays on a screen with no bottom bar`() {
        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.onNodeWithTag("mini").assertIsDisplayed()
    }
}
