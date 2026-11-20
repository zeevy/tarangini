/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.search

import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
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

/** The search screen at the narrowest width, in light and dark. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class SearchScreenshotTest {
    private val results = SearchResults(
        songs = listOf(
            testSong(1, "Morning raga", artist = "Ravi Shankar"),
            testSong(2, "A very long title of a song that does not fit on one line", artist = ""),
        ),
        albums = listOf(testAlbum(1, "Morning Ragas", artist = "Ravi Shankar", songCount = 8)),
        artists = listOf(testArtist(1, "Morning Star", songCount = 1)),
    )

    @Test
    fun `before a search`() = both("search_start", SearchUiState())

    @Test
    fun results() = both("search_results", SearchUiState("mor", results))

    @Test
    fun `no result`() = both("search_none", SearchUiState("a text that finds nothing at all", SearchResults()))

    private fun both(name: String, state: SearchUiState) {
        listOf("light" to false, "dark" to true).forEach { (theme, dark) ->
            val picture = File("src/test/screenshots/${name}_$theme.png")
            // Without this, a test with no saved picture passes: there is nothing to compare with.
            assertTrue(
                "No saved picture $picture. Run the tests with -PrecordScreenshots.",
                recording || picture.exists(),
            )
            captureRoboImage(picture.path) {
                TaranginiTheme(darkTheme = dark, dynamicColor = false) {
                    SearchContent(state.searched, state, SongRowActions({}, {}, {}), {}, {}, {}, {})
                }
            }
        }
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
