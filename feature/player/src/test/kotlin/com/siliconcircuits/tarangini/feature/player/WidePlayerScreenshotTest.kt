/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.nowplaying.DisplayParts
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingActions
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingContent
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingUiState
import com.siliconcircuits.tarangini.feature.player.nowplaying.ShownVisual
import com.siliconcircuits.tarangini.feature.player.queue.QueueUiState
import kotlin.math.sin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The player on wide screens, in both looks: a phone on its side and a tablet held flat (the card
 * and Up next side by side), and a tablet held upright (a bigger time and visual).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidePlayerScreenshotTest {
    private val actions = NowPlayingActions({}, {}, {}, {}, {}, {})
    private val song = NowPlayingUiState(
        title = "Bhairavi",
        artist = "Ravi Shankar",
        album = "Morning Ragas",
        artworkUri = null,
        showPause = true,
        isBuffering = false,
        positionMs = 65_000,
        durationMs = 185_000,
        shuffle = true,
        repeat = RepeatMode.ALL,
        isFavourite = true,
        albumId = 3,
        place = 2,
        queueSize = 4,
        source = QueueSource(QueueSourceKind.ALBUM, "Morning Ragas"),
    )
    private val queue = QueueUiState(
        items = listOf(
            PlaybackItem("a", "Morning raga", "Ravi Shankar", durationMs = 200_000),
            PlaybackItem("b", "Bhairavi", "Ravi Shankar", durationMs = 185_000),
            PlaybackItem("c", "Yaman", "Hariprasad Chaurasia", durationMs = 410_000),
            PlaybackItem("d", "Evening raga", "Zakir Hussain", durationMs = 190_000),
        ),
        currentIndex = 1,
    )
    private val sound = ShownVisual(
        bands = List(VisualFrame.BAND_COUNT) { (0.8f - it * 0.02f + 0.12f * sin(it * 0.9f)).coerceIn(0f, 1f) },
        peaks = List(VisualFrame.BAND_COUNT) { (0.9f - it * 0.02f + 0.12f * sin(it * 0.9f)).coerceIn(0f, 1f) },
        wave = List(VisualFrame.WAVE_POINTS) { 0f },
    )
    private val bars = DisplayParts(VisualMode.BARS, shown = { sound })

    @Test
    @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
    fun `a phone on its side`() = shot("player_landscape")

    @Test
    @Config(qualifiers = "w800dp-h360dp-land-night-xhdpi")
    fun `a phone on its side in the dark look`() = shot("player_landscape_dark")

    @Test
    @Config(qualifiers = "w1200dp-h800dp-land-mdpi")
    fun `a tablet held flat`() = shot("player_tablet_landscape")

    @Test
    @Config(qualifiers = "w1200dp-h800dp-land-night-mdpi")
    fun `a tablet held flat in the dark look`() = shot("player_tablet_landscape_dark")

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-mdpi")
    fun `a tablet held upright`() = shot("player_tablet_portrait")

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port-night-mdpi")
    fun `a tablet held upright in the dark look`() = shot("player_tablet_portrait_dark")

    private fun shot(name: String) = shot(name) { NowPlayingContent(song, actions, parts = bars, queue = queue) }

    private fun shot(name: String, content: @Composable () -> Unit) {
        captureRoboImage(savedPicture(name).path) {
            TaranginiTheme { content() }
        }
    }
}
