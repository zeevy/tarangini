/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.core.ui.theme.glassBackground
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

/** The mini player at the narrowest width. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class MiniPlayerScreenshotTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val song = MiniPlayerUiState(
        title = "A very long title of a song that does not fit on one line",
        artist = "An artist with a long name that does not fit",
        artworkUri = null,
        showPause = true,
        isBuffering = false,
        progress = 0.4f,
    )
    private val paused = song.copy(title = "Bhairavi", artist = "Ravi Shankar", showPause = false)
    private val buffering =
        MiniPlayerUiState("Radio Mirchi", null, null, showPause = true, isBuffering = true, progress = null)

    @Test
    fun `playing`() = capture("mini_player_playing", song)

    @Test
    fun `paused`() = capture("mini_player_paused", paused)

    @Test
    fun `buffering`() = capture("mini_player_buffering", buffering)

    @Test
    fun `playing with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        capture("mini_player_largest_font", song)
    }

    private fun capture(name: String, state: MiniPlayerUiState) {
        val picture = savedPicture("$name")
        // The line of the buffering state moves for ever. The clock is held, so the picture is of
        // one fixed moment and the test does not wait for the line to stop.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TaranginiTheme { MiniPlayerBar(state, {}, {}, {}, Modifier.glassBackground()) }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onRoot().captureRoboImage(picture.path)
    }
}
