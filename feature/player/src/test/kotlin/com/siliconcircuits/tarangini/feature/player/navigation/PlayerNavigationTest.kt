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

/** Which screens count as screens of the player. Plain texts stand in for the screens themselves. */
@RunWith(RobolectricTestRunner::class)
class PlayerNavigationTest {
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

    private lateinit var navController: NavHostController

    private fun onPlayerScreen(): Boolean? = composeRule.runOnUiThread<Boolean?> {
        navController.currentDestination?.isPlayerScreen()
    }

    private fun go(move: NavHostController.() -> Unit) {
        composeRule.runOnUiThread { navController.move() }
        composeRule.waitForIdle()
    }

    @Test
    fun `now playing and the queue are screens of the player, another screen is not`() {
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = OtherScreen) {
                composable<OtherScreen> { Text("other") }
                composable<NowPlayingRoute> { Text("now playing") }
                composable<QueueRoute> { Text("queue") }
            }
        }
        composeRule.waitForIdle()
        assertEquals(false, onPlayerScreen())

        go {
            navigateToNowPlaying()
            // A second tap that comes at once opens nothing more.
            navigateToNowPlaying()
        }
        assertEquals(true, onPlayerScreen())

        go { navigate(QueueRoute) }
        assertEquals(true, onPlayerScreen())

        go {
            popBackStack()
            popBackStack()
        }
        assertEquals(false, onPlayerScreen())
    }
}

@Serializable
internal data object OtherScreen
