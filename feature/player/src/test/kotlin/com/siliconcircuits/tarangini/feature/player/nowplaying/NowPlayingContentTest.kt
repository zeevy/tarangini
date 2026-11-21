/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.checkAccessibility
import org.junit.Assert.assertEquals
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

// A screen as high as a phone: on the small default screen of the tests the buttons are below the fold.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class NowPlayingContentTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val registerActivity = RegisterComponentActivityRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(registerActivity).around(composeRule)

    private val done = mutableListOf<String>()
    private val actions = NowPlayingActions(
        onTogglePlay = { done += "toggle" },
        onNext = { done += "next" },
        onPrevious = { done += "previous" },
        onSeek = { done += "seek $it" },
        onToggleShuffle = { done += "shuffle" },
        onCycleRepeat = { done += "repeat" },
    )
    private val options = PlaybackOptionActions(
        onSetSpeed = { done += "speed $it" },
        onSleepAfter = { done += "sleep $it" },
        onSleepAtEnd = { done += "sleep at end" },
        onCancelSleep = { done += "sleep off" },
    )
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

    private fun show(state: NowPlayingUiState?) {
        composeRule.setContent {
            TaranginiTheme {
                NowPlayingContent(state, actions, { done += "back" }, { done += "queue" }, options = options)
            }
        }
    }

    @Test
    fun `a song shows its texts, the time played and its length`() {
        show(song)

        listOf("Bhairavi", "Ravi", "Morning Ragas", "1:05", "3:05").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    private fun openOption(line: String) {
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText(line).performClick()
    }

    @Test
    fun `the menu says what speed and timer are set, and a pick in their list is passed on`() {
        show(song.copy(speed = 1.5f, sleep = SleepState.Timed(125_000)))

        openOption("Speed 1.5x")
        composeRule.onNodeWithText("2x").performClick()
        openOption("Sleep in 2:05")
        composeRule.onNodeWithText("30 minutes").performClick()
        openOption("Sleep in 2:05")
        composeRule.onNodeWithText("At the end of this song").performClick()
        openOption("Sleep in 2:05")
        composeRule.onNodeWithText("Turn the timer off").performClick()

        assertEquals(listOf("speed 2.0", "sleep 30", "sleep at end", "sleep off"), done)
    }

    @Test
    fun `a live stream has no speed to set`() {
        show(song.copy(durationMs = null))

        composeRule.onNodeWithContentDescription("More options").performClick()

        composeRule.onNodeWithText("Speed 1x").assertDoesNotExist()
        composeRule.onNodeWithText("Sleep timer").assertIsDisplayed()
    }

    @Test
    fun `each button does its own thing`() {
        show(song)

        listOf("Shuffle", "Previous", "Pause", "Next", "Repeat is off", "Open the queue", "Back").forEach {
            composeRule.onNodeWithContentDescription(it).performClick()
        }

        assertEquals(listOf("shuffle", "previous", "toggle", "next", "repeat", "queue", "back"), done)
    }

    @Test
    fun `a move of the seek bar seeks once, to the place it was let go at`() {
        show(song)

        composeRule.onNodeWithContentDescription("Position in the song")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(120_000f) }

        assertEquals(listOf("seek 120000"), done)
    }

    @Test
    fun `the repeat button and the play button name what they show`() {
        show(song.copy(showPause = false, repeat = RepeatMode.ONE))

        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Repeat this song").assertIsDisplayed()
    }

    @Test
    fun `a station has no seek bar, no shuffle and no repeat, and says that it is live`() {
        show(song.copy(title = "Radio Mirchi", artist = "A song on air", album = null, durationMs = null))

        composeRule.onNodeWithText("Radio Mirchi").assertIsDisplayed()
        composeRule.onNodeWithText("A song on air").assertIsDisplayed()
        composeRule.onNodeWithText("Live").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Position in the song").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Shuffle").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Repeat is off").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun `a station with no song title shows its name and no line for an artist`() {
        show(song.copy(title = "Radio Mirchi", artist = null, album = null, durationMs = null))

        composeRule.onNodeWithText("Radio Mirchi").assertIsDisplayed()
        composeRule.onNodeWithText("Unknown artist").assertDoesNotExist()
        composeRule.onNodeWithText("Live").assertIsDisplayed()
    }

    @Test
    fun `a station the player gave up on says why, and the button tries again`() {
        show(song.copy(durationMs = null, showPause = false, error = PlaybackError.NETWORK))

        composeRule.onNodeWithText("No connection to the station. Check the network.").assertIsDisplayed()
        composeRule.onNodeWithText("Live").assertDoesNotExist()
        composeRule.onNodeWithText("Try again").performClick()

        assertEquals(listOf("toggle"), done)
    }

    @Test
    fun `a song with an error shows the reason in place of the seek bar`() {
        show(song.copy(showPause = false, error = PlaybackError.FILE_NOT_AVAILABLE))

        composeRule.onNodeWithText("This file is not there any more.").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun `a station that waits for data says that it loads`() {
        show(song.copy(durationMs = null, isBuffering = true))

        composeRule.onNodeWithText("Loading").assertIsDisplayed()
    }

    @Test
    fun `with nothing loaded the screen says so, and the queue and Back still work`() {
        show(null)

        composeRule.onNodeWithText("Nothing is playing").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open the queue").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("queue", "back"), done)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the screen passes the accessibility checks`() {
        show(song)

        composeRule.checkAccessibility()
    }
}
