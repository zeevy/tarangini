/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.lyrics

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.library.FakeSongLyrics
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingActions
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingContent
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingUiState
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class LyricsTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val lyrics = FakeSongLyrics().apply { this.lyrics["song:1"] = LYRICS }
    private val song = NowPlayingUiState(
        title = "Bhairavi",
        artist = "Ravi",
        album = "Morning Ragas",
        artworkUri = null,
        showPause = true,
        isBuffering = false,
        positionMs = 65_000,
        durationMs = 185_000,
        shuffle = false,
        repeat = RepeatMode.OFF,
    )

    private fun TestScope.watched(): LyricsViewModel = LyricsViewModel(playback, lyrics).also { viewModel ->
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()
    }

    private fun playing(id: String, positionMs: Long = 0) =
        PlaybackState(queue = listOf(PlaybackItem(id, "Bhairavi")), currentIndex = 0, positionMs = positionMs)

    private fun show(lyrics: String?) = composeRule.setContent {
        TaranginiTheme { NowPlayingContent(song, NowPlayingActions({}, {}, {}, {}, {}, {}), {}, {}, lyrics = lyrics) }
    }

    @Test
    fun `the lyrics of the song that plays are read once, and a song with none has none`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = watched()

            playback.state.value = playing("song:1")
            runCurrent()
            playback.state.value = playing("song:1", positionMs = 9_000)
            runCurrent()
            val first = viewModel.state.value
            playback.state.value = playing("song:2")
            runCurrent()

            assertEquals(LYRICS, first)
            assertNull(viewModel.state.value)
            assertEquals(listOf("song:1", "song:2"), lyrics.asked)
        }

    @Test
    fun `while the next song is read the lyrics of the last one are gone`() = runTest(mainDispatcher.dispatcher) {
        lyrics.lyrics["song:3"] = "Other words"
        val viewModel = watched()
        playback.state.value = playing("song:1")
        runCurrent()
        lyrics.waitsFor = CompletableDeferred()

        playback.state.value = playing("song:3")
        runCurrent()
        val between = viewModel.state.value
        lyrics.waitsFor?.complete(Unit)
        runCurrent()

        assertNull(between)
        assertEquals("Other words", viewModel.state.value)
    }

    @Test
    fun `open lyrics close with the song and do not open again by themselves`() {
        var words by mutableStateOf<String?>(LYRICS)
        composeRule.setContent {
            TaranginiTheme {
                NowPlayingContent(song, NowPlayingActions({}, {}, {}, {}, {}, {}), {}, {}, lyrics = words)
            }
        }
        openMenu()
        composeRule.onNodeWithText("Lyrics").performClick()

        words = null
        composeRule.waitForIdle()
        words = "Words of the next song"
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Words of the next song").assertDoesNotExist()
    }

    @Test
    fun `with nothing loaded no file is read`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()

        assertNull(viewModel.state.value)
        assertEquals(emptyList<String>(), lyrics.asked)
    }

    private fun openMenu() = composeRule.onNodeWithContentDescription("More options").performClick()

    @Test
    fun `a song with no lyrics has no lyrics line in the menu`() {
        show(lyrics = null)

        openMenu()

        composeRule.onNodeWithText("Sleep timer").assertIsDisplayed()
        composeRule.onNodeWithText("Lyrics").assertDoesNotExist()
    }

    @Test
    fun `the lyrics line opens the lyrics, and OK closes them`() {
        show(LYRICS)

        openMenu()
        composeRule.onNodeWithText("Lyrics").performClick()
        composeRule.onNodeWithText(LYRICS).assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        composeRule.onNodeWithText(LYRICS).assertDoesNotExist()
    }

    @Test
    fun `the open lyrics with the largest font`() {
        val picture = savedPicture("now_playing_lyrics_largest_font")
        RuntimeEnvironment.setFontScale(LARGEST_FONT)
        show(List(LONG_LYRICS_LINES) { "Line ${it + 1} of a long song" }.joinToString("\n"))
        openMenu()
        composeRule.onNodeWithText("Lyrics").performClick()
        // The dialog opens in a later frame than the tap.
        composeRule.waitForIdle()

        captureScreenRoboImage(picture.path)
    }

    private companion object {
        const val LYRICS = "First line\nSecond line"
        const val LARGEST_FONT = 2f
        const val LONG_LYRICS_LINES = 40
    }
}
