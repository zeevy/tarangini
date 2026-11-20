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
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        playlists = listOf(Playlist(1, "Drive", 12), Playlist(2, "Morning", 1)),
    )

    @Test
    fun `first use`() = shot("home_first_use") { HomeContent(HomeUiState(isLoading = false), songActions, HomeLinks()) }

    @Test
    fun `every section`() = shot("home_full") { HomeContent(full, songActions, HomeLinks()) }

    @Test
    fun `only playlists`() = shot("home_playlists_only") {
        HomeContent(full.copy(paused = null, recent = emptyList()), songActions, HomeLinks())
    }

    private fun shot(name: String, body: @Composable () -> Unit) {
        val picture = File("src/test/screenshots/$name.png")
        // Without this, a test with no saved picture passes: there is nothing to compare with.
        assertTrue("No saved picture $picture. Run the tests with -PrecordScreenshots.", recording || picture.exists())
        captureRoboImage(picture.path) {
            TaranginiTheme { HomeScreen(onSettingsClick = {}) { body() } }
        }
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
