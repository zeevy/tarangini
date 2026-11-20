/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.navigation

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
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

/** The ways another feature opens an album or an artist, on a graph with plain screens. */
@RunWith(RobolectricTestRunner::class)
class LibraryNavigationTest {
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

    private fun showGraph() {
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = LibraryListsRoute) {
                composable<LibraryListsRoute> { Text("start") }
                composable<AlbumRoute> { Text("album") }
                composable<ArtistRoute> { Text("artist") }
            }
        }
        composeRule.waitForIdle()
    }

    private fun shownAlbum() = composeRule.runOnIdle { navController.currentBackStackEntry!!.toRoute<AlbumRoute>() }

    @Test
    fun `an album opens with its id and the texts of the top bar`() {
        showGraph()

        composeRule.runOnIdle {
            navController.navigateToAlbum(testAlbum(7, "Morning / Ragas? 100%", artist = "A & B #1"))
        }

        assertEquals(AlbumRoute(7, "Morning / Ragas? 100%", "A & B #1"), shownAlbum())
    }

    @Test
    fun `an album with no title and no artist opens`() {
        showGraph()

        composeRule.runOnIdle { navController.navigateToAlbum(testAlbum(7, "", artist = "")) }

        assertEquals(AlbumRoute(7, "", ""), shownAlbum())
    }

    @Test
    fun `a very long text is cut in the route`() {
        showGraph()

        composeRule.runOnIdle { navController.navigateToAlbum(testAlbum(7, "a".repeat(5_000))) }

        assertEquals(200, shownAlbum().title.length)
    }

    @Test
    fun `an artist opens once when it is asked for twice`() {
        showGraph()

        composeRule.runOnIdle {
            navController.navigateToArtist(testArtist(3, "Ravi"))
            navController.navigateToArtist(testArtist(3, "Ravi"))
        }

        val shown = composeRule.runOnIdle { navController.currentBackStackEntry!!.toRoute<ArtistRoute>() }
        assertEquals(ArtistRoute(3, "Ravi"), shown)
        composeRule.runOnIdle { navController.popBackStack() }
        val back = composeRule.runOnIdle {
            navController.currentBackStackEntry?.destination?.hasRoute<LibraryListsRoute>()
        }
        assertEquals(true, back)
    }
}
