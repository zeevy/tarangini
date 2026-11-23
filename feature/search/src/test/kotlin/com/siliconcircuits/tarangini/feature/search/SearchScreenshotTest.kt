/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.search

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import java.io.File
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The search screen at the narrowest width. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class SearchScreenshotTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val nothingFound = SearchUiState("a text that finds nothing at all", SearchResults())

    private val results = SearchResults(
        songs = listOf(
            testSong(1, "Morning raga", artist = "Ravi Shankar"),
            testSong(2, "A very long title of a song that does not fit on one line", artist = ""),
        ),
        albums = listOf(testAlbum(1, "Morning Ragas", artist = "Ravi Shankar", songCount = 8)),
        artists = listOf(testArtist(1, "Morning Star", songCount = 1)),
    )

    @Test
    fun `before a search`() = capture("search_start", SearchUiState())

    @Test
    fun `results`() = capture("search_results", SearchUiState("mor", results))

    @Test
    @Config(qualifiers = "+night")
    fun `results in the dark look`() = capture("search_results_dark", SearchUiState("mor", results))

    @Test
    fun `no result`() = capture("search_none", nothingFound)

    @Test
    fun `results with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        capture("search_results_largest_font", SearchUiState("mor", results))
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `results on a wide screen`() = capture("search_results_wide", SearchUiState("mor", results))

    private fun capture(name: String, state: SearchUiState) {
        val picture = savedPicture("$name")
        // The cursor of the empty field blinks. The clock is held, so the picture is of one fixed
        // moment and does not depend on how fast the test ran.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TaranginiTheme {
                SearchContent(state.searched, state, SongRowActions({}, {}, {}), {}, {}, {}, {})
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onRoot().captureRoboImage(picture.path)
    }
}
