/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingActions
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingContent
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingUiState
import com.siliconcircuits.tarangini.feature.player.queue.QueueActions
import com.siliconcircuits.tarangini.feature.player.queue.QueueContent
import com.siliconcircuits.tarangini.feature.player.queue.QueueUiState
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The now playing screen and the queue at the narrowest width, in light and dark, and on a wide screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class PlayerScreenshotTest {
    private val actions = NowPlayingActions({}, {}, {}, {}, {}, {})
    private val song = NowPlayingUiState(
        title = "A very long title of a song that does not fit on one line and not on two",
        artist = "Ravi Shankar",
        album = "Morning Ragas",
        artworkUri = null,
        showPause = true,
        isBuffering = false,
        positionMs = 65_000,
        durationMs = 185_000,
        shuffle = true,
        repeat = RepeatMode.ONE,
    )
    private val station = song.copy(title = "Radio Mirchi", artist = "A song on air", album = null, durationMs = null)
    private val queue = QueueUiState(
        items = listOf(
            PlaybackItem("a", "Morning raga", "Ravi Shankar"),
            PlaybackItem("b", "A very long title of a song that does not fit on one line", null),
            PlaybackItem("c", "Evening raga", "Zakir Hussain"),
        ),
        currentIndex = 1,
    )

    @Test
    fun `a song`() = both("now_playing_song") { NowPlayingContent(song, actions, {}, {}) }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `a song on a wide screen`() = both("now_playing_song_wide") { NowPlayingContent(song, actions, {}, {}) }

    @Test
    fun `a station`() = both("now_playing_station") { NowPlayingContent(station, actions, {}, {}) }

    @Test
    fun `a station with no connection`() = both("now_playing_station_error") {
        val failed = station.copy(artist = null, showPause = false, error = PlaybackError.NETWORK)
        NowPlayingContent(failed, actions, {}, {})
    }

    @Test
    fun `nothing loaded`() = both("now_playing_empty") { NowPlayingContent(null, actions, {}, {}) }

    @Test
    fun `the queue`() = both("queue") { QueueContent(queue, QueueActions({}, { _, _ -> }, {}), {}) }

    private fun both(name: String, content: @Composable () -> Unit) {
        listOf("light" to false, "dark" to true).forEach { (theme, dark) ->
            val picture = File("src/test/screenshots/${name}_$theme.png")
            // Without this, a test with no saved picture passes: there is nothing to compare with.
            assertTrue(
                "No saved picture $picture. Run the tests with -PrecordScreenshots.",
                recording || picture.exists(),
            )
            captureRoboImage(picture.path) {
                TaranginiTheme(darkTheme = dark, dynamicColor = false) { content() }
            }
        }
    }
}

private val recording = System.getProperty("roborazzi.test.record").toBoolean()
