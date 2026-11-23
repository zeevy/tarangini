/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.feature.library.navigation.LibraryRoute
import com.siliconcircuits.tarangini.feature.player.navigation.NowPlayingRoute
import com.siliconcircuits.tarangini.testing.NoPlaybackServiceRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopLevelNavigationTest {
    @get:Rule(order = 0)
    val registerActivity = RegisterComponentActivityRule()

    @get:Rule(order = 0)
    val noPlaybackService = NoPlaybackServiceRule()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a tab keeps its state while the user is on another tab`() {
        composeRule.setContent { TwoTabs() }
        composeRule.onNodeWithText("player taps 0").performClick()
        composeRule.onNodeWithText("player taps 1").performClick()

        composeRule.onNodeWithText("go to library").performClick()
        composeRule.onNodeWithText("library").assertIsDisplayed()
        composeRule.onNodeWithText("go to player").performClick()

        composeRule.onNodeWithText("player taps 2").assertIsDisplayed()
    }

    @Test
    fun `choosing the tab that is open does not reset it`() {
        composeRule.setContent { TwoTabs() }
        composeRule.onNodeWithText("player taps 0").performClick()

        composeRule.onNodeWithText("go to player").performClick()

        composeRule.onNodeWithText("player taps 1").assertIsDisplayed()
    }

    @Composable
    private fun TwoTabs() {
        val navController = rememberNavController()
        Column {
            Button(onClick = { navController.navigateToTopLevel(TopLevelDestination.PLAYER) }) { Text("go to player") }
            Button(onClick = { navController.navigateToTopLevel(TopLevelDestination.LIBRARY) }) {
                Text("go to library")
            }
            NavHost(navController = navController, startDestination = NowPlayingRoute) {
                composable<NowPlayingRoute> {
                    var taps by rememberSaveable { mutableIntStateOf(0) }
                    Button(onClick = { taps++ }) { Text("player taps $taps") }
                }
                composable<LibraryRoute> { Text("library") }
            }
        }
    }
}
