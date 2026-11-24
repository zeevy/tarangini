/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.nowplaying.CompactSong
import com.siliconcircuits.tarangini.feature.player.nowplaying.ShownVisual
import kotlin.math.sin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The mini player at the narrowest width. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class MiniPlayerScreenshotTest {
    private val song = CompactSong(
        title = "A very long title of a song that does not fit on one line",
        artist = "An artist with a long name that does not fit",
        positionMs = 65_000,
        durationMs = 185_000,
        showPause = true,
        isBuffering = false,
    )
    private val paused = song.copy(title = "Bhairavi", artist = "Ravi Shankar", showPause = false)
    private val station = CompactSong("Radio Mirchi", "A song on air", 0, null, showPause = true, isBuffering = true)
    private val sound = ShownVisual(
        bands = List(VisualFrame.BAND_COUNT) { (0.7f + 0.25f * sin(it * 0.8f)).coerceIn(0f, 1f) },
        peaks = List(VisualFrame.BAND_COUNT) { 1f },
        wave = List(VisualFrame.WAVE_POINTS) { 0f },
    )

    @Test
    fun `playing`() = capture("mini_player_playing", song)

    @Test
    @Config(qualifiers = "+night")
    fun `playing in the dark look`() = capture("mini_player_playing_dark", song)

    @Test
    fun `paused`() = capture("mini_player_paused", paused)

    @Test
    @Config(qualifiers = "+night")
    fun `paused in the dark look`() = capture("mini_player_paused_dark", paused)

    @Test
    fun `a station that loads`() = capture("mini_player_buffering", station)

    @Test
    fun `playing with the largest font`() {
        RuntimeEnvironment.setFontScale(2f)
        capture("mini_player_largest_font", song)
    }

    @Test
    @Config(qualifiers = "+night")
    fun `playing with the largest font in the dark look`() {
        RuntimeEnvironment.setFontScale(2f)
        capture("mini_player_largest_font_dark", song)
    }

    private fun capture(name: String, state: CompactSong) {
        captureRoboImage(savedPicture(name).path) {
            TaranginiTheme { MiniPlayerBar(state, {}, {}, {}, shown = { sound }) }
        }
    }
}
