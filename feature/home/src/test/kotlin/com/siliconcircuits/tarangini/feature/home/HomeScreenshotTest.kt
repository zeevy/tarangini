/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home at the narrowest width. The app has one dark look, so each state has one picture. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class HomeScreenshotTest {
    private val songActions = SongRowActions({}, {}, {})
    private val full = HomeUiState(
        isLoading = false,
        paused = PlaybackItem("test:1", "A very long title of a song that does not fit", "Ravi Shankar", "Album", null),
        recent = listOf(
            testSong(1, "Bhairavi", artist = "Ravi Shankar"),
            testSong(2, "ఎందరో మహానుభావులు", artist = ""),
        ),
        mostPlayed = listOf(testSong(3, "Yaman", artist = "Ravi Shankar")),
        playlists = listOf(Playlist(1, "Drive", 12), Playlist(2, "Morning", 1)),
    )

    @Test
    fun `first use`() = shot("home_first_use") {
        HomeContent(HomeUiState(isLoading = false), songActions, songActions, HomeLinks())
    }

    @Test
    fun `every section`() = shot("home_full") { HomeContent(full, songActions, songActions, HomeLinks()) }

    @Test
    @Config(qualifiers = "+night")
    fun `every section in the dark look`() = shot("home_full_dark") {
        HomeContent(full, songActions, songActions, HomeLinks())
    }

    @Test
    fun `only playlists`() = shot("home_playlists_only") {
        HomeContent(full.copy(paused = null, recent = emptyList()), songActions, songActions, HomeLinks())
    }

    @Test
    fun `every section with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        shot("home_full_largest_font") { HomeContent(full, songActions, songActions, HomeLinks()) }
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `every section on a wide screen`() = shot("home_full_wide") {
        HomeContent(full, songActions, songActions, HomeLinks())
    }

    @Test
    fun `first use with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        shot("home_first_use_largest_font") {
            HomeContent(HomeUiState(isLoading = false), songActions, songActions, HomeLinks())
        }
    }

    private fun shot(name: String, body: @Composable () -> Unit) {
        val picture = savedPicture("$name")
        captureRoboImage(picture.path) {
            TaranginiTheme { HomeScreen(onSettingsClick = {}) { body() } }
        }
    }
}
