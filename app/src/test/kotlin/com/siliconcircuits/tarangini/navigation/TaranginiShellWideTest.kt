/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.testing.NoPlaybackServiceRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TaranginiShellWideTest {
    @get:Rule(order = 0)
    val registerActivity = RegisterComponentActivityRule()

    @get:Rule(order = 0)
    val noPlaybackService = NoPlaybackServiceRule()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = TABLET)
    fun `a tablet shows the player pane beside another tab and no mini player`() {
        showShell()

        // The player tab is the player itself: no pane beside it.
        composeRule.onNodeWithTag(TAB).assertIsDisplayed()
        composeRule.onNodeWithTag(PANE).assertDoesNotExist()
        composeRule.onNodeWithTag(MINI).assertDoesNotExist()
        composeRule.onNode(tab("Library")).performClick()
        composeRule.onNode(tab("Library")).assertIsSelected()
        composeRule.onNodeWithTag(PANE).assertIsDisplayed()
        composeRule.onNodeWithTag(MINI).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `a screen outside the tabs fills the window with no rail or pane`() {
        showShell()

        composeRule.onNodeWithTag(TAB).performClick()

        composeRule.onNode(tab("Player")).assertDoesNotExist()
        composeRule.onNodeWithTag(PANE).assertDoesNotExist()
        composeRule.onNodeWithTag(MINI).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = OPEN_FOLDABLE)
    fun `an open foldable has the rail and the mini player but no pane`() {
        showShell()

        composeRule.onNode(tab("Favourites")).performClick()

        composeRule.onNode(tab("Favourites")).assertIsSelected()
        composeRule.onNodeWithTag(MINI).assertIsDisplayed()
        composeRule.onNodeWithTag(PANE).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = OPEN_FOLDABLE)
    fun `the open tab stays open when the activity is made again after a fold`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { TaranginiTheme { TaranginiShell() } }
        composeRule.onNode(tab("Library")).performClick()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNode(tab("Library")).assertIsSelected()
    }

    @Test
    @Config(qualifiers = TABLET)
    fun `the tablet layout`() {
        capture("shell_tablet")
    }

    @Test
    @Config(qualifiers = OPEN_FOLDABLE)
    fun `the open foldable layout`() {
        capture("shell_foldable")
    }

    // The tabs show once the navigation has its first screen, so the picture waits for that.
    private fun capture(name: String) {
        showShell()
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(savedPicture(name).path)
    }

    private fun showShell() = composeRule.setContent { TaranginiTheme { Shell() } }

    private fun tab(name: String) = hasText(name) and isSelectable()
}

@Composable
private fun Shell() {
    TaranginiShell(
        miniPlayer = { _, modifier -> Text("mini player", modifier.testTag(MINI)) },
        // The tab gets a plain modifier, the pane one with its width. A tap on the tab opens Settings.
        player = { targets, modifier ->
            if (modifier == Modifier) {
                Text("player tab", Modifier.testTag(TAB).clickable(onClick = targets.onOpenSettings))
            } else {
                Text("player pane", modifier.testTag(PANE))
            }
        },
    )
}

private const val MINI = "mini"
private const val PANE = "pane"
private const val TAB = "tab"
private const val TABLET = "w1280dp-h800dp-land-mdpi"
private const val OPEN_FOLDABLE = "w673dp-h841dp-xhdpi"
