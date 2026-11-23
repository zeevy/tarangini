/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.nowplaying.CompactCard
import com.siliconcircuits.tarangini.feature.player.nowplaying.DisplayParts
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingActions
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingContent
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingUiState
import com.siliconcircuits.tarangini.feature.player.nowplaying.ShownVisual
import com.siliconcircuits.tarangini.feature.player.queue.QueueUiState
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The player tab in each of its states, on a phone of 360 by 800 dp, at the narrowest width and on a wide screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PlayerScreenshotTest {
    private val actions = NowPlayingActions({}, {}, {}, {}, {}, {})
    private val song = NowPlayingUiState(
        title = "A very long title of a song that does not fit on one line and not on two",
        artist = "Ravi Shankar",
        album = "Morning Ragas",
        artworkUri = null,
        // Paused, so the long title rests at its start; the visual is shown as it plays.
        showPause = false,
        isBuffering = false,
        positionMs = 65_000,
        durationMs = 185_000,
        shuffle = true,
        repeat = RepeatMode.ONE,
        isFavourite = true,
        albumId = 3,
        place = 2,
        queueSize = 4,
        source = QueueSource(QueueSourceKind.ALBUM, "Morning Ragas"),
    )
    private val playing = song.copy(title = "Bhairavi", showPause = true)
    private val station = playing.copy(
        title = "Radio Mirchi",
        artist = "A song on air",
        album = null,
        albumId = null,
        durationMs = null,
        isFavourite = null,
        place = 1,
        queueSize = 1,
        source = null,
    )
    private val queue = QueueUiState(
        items = listOf(
            PlaybackItem("a", "Morning raga", "Ravi Shankar", durationMs = 200_000),
            PlaybackItem("b", "Bhairavi", "Ravi Shankar", durationMs = 185_000),
            PlaybackItem("c", "A very long title of a song that does not fit on one line", null, durationMs = 410_000),
            PlaybackItem("d", "Evening raga", "Zakir Hussain", durationMs = 190_000),
        ),
        currentIndex = 1,
    )
    private val sound = ShownVisual(
        bands = List(VisualFrame.BAND_COUNT) { (0.85f - it * 0.02f + 0.12f * sin(it * 0.9f)).coerceIn(0f, 1f) },
        peaks = List(VisualFrame.BAND_COUNT) { (0.92f - it * 0.02f + 0.12f * sin(it * 0.9f)).coerceIn(0f, 1f) },
        wave = List(VisualFrame.WAVE_POINTS) { (0.7 * sin(it * 2 * PI / 24) * sin(it * PI / 96)).toFloat() },
    )
    private val bars = DisplayParts(VisualMode.BARS, shown = { sound })

    @Test
    fun `a song that plays`() = shot("player_song") { NowPlayingContent(playing, actions, parts = bars, queue = queue) }

    @Test
    @Config(qualifiers = "+night")
    fun `a song that plays in the dark look`() = shot("player_song_dark") {
        NowPlayingContent(playing, actions, parts = bars, queue = queue)
    }

    @Test
    fun `a long title in a pause`() = shot("player_paused") {
        NowPlayingContent(song, actions, parts = bars, queue = queue)
    }

    @Test
    fun `the wave with every tag on`() = shot("player_wave_tags") {
        val tagged = playing.copy(sleep = SleepState.Timed(600_000), speed = 1.25f)
        NowPlayingContent(
            tagged,
            actions,
            parts = DisplayParts(VisualMode.WAVE, levelling = true, shown = {
                sound
            }),
            queue = queue,
        )
    }

    @Test
    @Config(qualifiers = "+night")
    fun `the wave in the dark look`() = shot("player_wave_dark") {
        NowPlayingContent(playing, actions, parts = DisplayParts(VisualMode.WAVE, shown = { sound }), queue = queue)
    }

    @Test
    fun `the visual off, from the favourites`() = shot("player_visual_off") {
        val fromFavourites = playing.copy(source = QueueSource(QueueSourceKind.FAVOURITES))
        NowPlayingContent(fromFavourites, actions, parts = DisplayParts(VisualMode.OFF), queue = queue)
    }

    @Test
    fun `a station`() = shot("player_station") {
        NowPlayingContent(
            station,
            actions,
            parts = bars,
            queue = QueueUiState(listOf(PlaybackItem("s", "Radio Mirchi", isStream = true)), 0),
        )
    }

    @Test
    fun `a song that cannot be played`() = shot("player_error") {
        NowPlayingContent(song.copy(error = PlaybackError.FILE_NOT_AVAILABLE), actions, parts = bars, queue = queue)
    }

    @Test
    fun `nothing loaded`() = shot("player_empty") { NowPlayingContent(null, actions) }

    @Test
    @Config(qualifiers = "+night")
    fun `nothing loaded in the dark look`() = shot("player_empty_dark") { NowPlayingContent(null, actions) }

    @Test
    fun `the compact card`() = shot("player_compact") { CompactCard(playing, actions, { sound }, {}) }

    @Test
    @Config(qualifiers = "+night")
    fun `the compact card in the dark look`() = shot("player_compact_dark") {
        CompactCard(playing, actions, { sound }, {})
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun `a song at the narrowest width`() = shot("player_song_narrow") {
        NowPlayingContent(song, actions, parts = bars, queue = queue)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun `a song with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        shot("player_song_largest_font") { NowPlayingContent(song, actions, parts = bars, queue = queue) }
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun `a song on a wide screen`() = shot("player_song_wide") {
        NowPlayingContent(song, actions, parts = bars, queue = queue)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        captureRoboImage(savedPicture(name).path) {
            TaranginiTheme { content() }
        }
    }
}
