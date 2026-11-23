/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.feature.player.nowplaying.PlayerTargets
import kotlinx.serialization.Serializable
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

/** Which screen counts as the player. Plain texts stand in for the screens themselves. */
@RunWith(RobolectricTestRunner::class)
class PlayerNavigationTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private lateinit var navController: NavHostController

    private fun onPlayerScreen(): Boolean? = composeRule.runOnUiThread<Boolean?> {
        navController.currentDestination?.isPlayerScreen()
    }

    private fun go(move: NavHostController.() -> Unit) {
        composeRule.runOnUiThread { navController.move() }
        composeRule.waitForIdle()
    }

    @Test
    fun `the player tab is the screen of the player, another screen is not`() {
        var shown: PlayerTargets? = null
        val targets = PlayerTargets()
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = OtherScreen) {
                composable<OtherScreen> { Text("other") }
                playerScreen(targets) { shown = it }
            }
        }
        composeRule.waitForIdle()
        assertEquals(false, onPlayerScreen())

        go { navigate(NowPlayingRoute) }
        assertEquals(true, onPlayerScreen())
        // The tab gets the places the app hands in.
        assertEquals(targets, shown)

        go { popBackStack() }
        assertEquals(false, onPlayerScreen())
    }
}

@Serializable
internal data object OtherScreen
