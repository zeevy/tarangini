/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class HomeContentTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val full = HomeUiState(
        isLoading = false,
        paused = PlaybackItem("test:1", "Yaman", "Ravi Shankar", "Album", null),
        recent = listOf(testSong(1, "Bhairavi", artist = "Ravi Shankar")),
        playlists = listOf(Playlist(1, "Drive", 12)),
    )

    private fun show(state: HomeUiState) = composeRule.setContent {
        // Inside the frame of the screen, which gives the dark background and the text colour.
        TaranginiTheme {
            val actions = SongRowActions({}, {}, {})
            HomeScreen(onSettingsClick = {}) { HomeContent(state, actions, actions, HomeLinks()) }
        }
    }

    @Test
    fun `Home with every section passes the accessibility checks`() {
        show(full)

        composeRule.checkAccessibility()
    }

    @Test
    @Config(qualifiers = "+night")
    fun `Home with every section passes the accessibility checks in the dark look`() {
        show(full)

        composeRule.checkAccessibility()
    }

    @Test
    fun `the first use screen passes the accessibility checks`() {
        show(HomeUiState(isLoading = false))

        composeRule.checkAccessibility()
    }
}
