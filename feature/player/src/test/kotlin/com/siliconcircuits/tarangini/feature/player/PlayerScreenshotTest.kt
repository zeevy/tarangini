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
import com.siliconcircuits.tarangini.core.testing.savedPicture
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The now playing screen and the queue at the narrowest widthand on a wide screen. */
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
    fun `a song`() = shot("now_playing_song") { NowPlayingContent(song, actions, {}, {}) }

    @Test
    @Config(qualifiers = "+night")
    fun `a song in the dark look`() = shot("now_playing_song_dark") { NowPlayingContent(song, actions, {}, {}) }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `a song on a wide screen`() = shot("now_playing_song_wide") { NowPlayingContent(song, actions, {}, {}) }

    @Test
    fun `a station`() = shot("now_playing_station") { NowPlayingContent(station, actions, {}, {}) }

    @Test
    fun `a station with no connection`() = shot("now_playing_station_error") {
        val failed = station.copy(artist = null, showPause = false, error = PlaybackError.NETWORK)
        NowPlayingContent(failed, actions, {}, {})
    }

    @Test
    fun `nothing loaded`() = shot("now_playing_empty") { NowPlayingContent(null, actions, {}, {}) }

    @Test
    fun `the queue`() = shot("queue") { QueueContent(queue, QueueActions({}, { _, _ -> }, {}), {}) }

    @Test
    @Config(qualifiers = "+night")
    fun `the queue in the dark look`() = shot("queue_dark") {
        QueueContent(queue, QueueActions({}, { _, _ -> }, {}), {})
    }

    @Test
    fun `a song with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        shot("now_playing_song_largest_font") { NowPlayingContent(song, actions, {}, {}) }
    }

    @Test
    fun `the queue with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        shot("queue_largest_font") { QueueContent(queue, QueueActions({}, { _, _ -> }, {}), {}) }
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-xhdpi")
    fun `the queue on a wide screen`() = shot("queue_wide") {
        QueueContent(queue, QueueActions({}, { _, _ -> }, {}), {})
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        val picture = savedPicture("$name")
        captureRoboImage(picture.path) {
            TaranginiTheme { content() }
        }
    }
}
