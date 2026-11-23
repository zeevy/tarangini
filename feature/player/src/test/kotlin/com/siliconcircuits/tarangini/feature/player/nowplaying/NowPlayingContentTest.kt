/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.checkAccessibility
import com.siliconcircuits.tarangini.feature.player.queue.QueueUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// A screen as high as a phone: on the small default screen of the tests the keys are below the fold.
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
        onSeekBy = { done += "seek by $it" },
        onToggleShuffle = { done += "shuffle" },
        onCycleRepeat = { done += "repeat" },
        onToggleFavourite = { done += "favourite" },
    )
    private val targets = PlayerTargets(
        onOpenSearch = { done += "search" },
        onOpenSettings = { done += "settings" },
        onOpenLibrary = { done += "library" },
        onOpenAlbum = { id, title, artist -> done += "album $id $title $artist" },
    )
    private val options = PlaybackOptionActions(
        onSetSpeed = { done += "speed $it" },
        onSleepAfter = { done += "sleep $it" },
        onSleepAtEnd = { done += "sleep at end" },
        onCancelSleep = { done += "sleep off" },
        onOpenSettings = { done += "settings" },
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
    private val queue = QueueUiState(listOf(PlaybackItem("a", "Bhairavi", "Ravi", durationMs = 185_000)), 0)

    private fun show(
        state: NowPlayingUiState?,
        parts: DisplayParts = DisplayParts(onCycleVisual = { done += "visual" }),
        extras: PlayerExtras = PlayerExtras(options = options, onShuffleAll = { done += "shuffle all" }),
    ) {
        composeRule.setContent {
            TaranginiTheme {
                NowPlayingContent(
                    state,
                    actions,
                    targets = targets,
                    parts = parts,
                    queue = if (state == null) QueueUiState() else queue,
                    player = extras,
                )
            }
        }
    }

    @Test
    fun `a song shows its texts, the time played and its length`() {
        show(song)

        listOf("Bhairavi", "Ravi · Morning Ragas", "3:05").forEach {
            composeRule.onAllNodesWithText(it, useUnmergedTree = true)[0].assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("1:05 played").assertIsDisplayed()
        composeRule.onNodeWithText("Now playing").assertIsDisplayed()
    }

    @Test
    fun `a tap on the time shows the time left, and a second tap the time played again`() {
        show(song)

        composeRule.onNodeWithContentDescription("1:05 played").performClick()
        composeRule.onNodeWithContentDescription("2:00 left").performClick()
        composeRule.onNodeWithContentDescription("1:05 played").assertIsDisplayed()
    }

    @Test
    fun `the place in the queue stands before the title`() {
        show(song.copy(place = 2, queueSize = 8))

        composeRule.onNodeWithText("2. Bhairavi", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `the tags show only what is on`() {
        show(song.copy(speed = 1.5f, sleep = SleepState.Timed(125_000)), DisplayParts(levelling = true))

        composeRule.onNodeWithContentDescription("Sleep timer on, Speed 1.5x, Volume levelling on").assertIsDisplayed()
    }

    @Test
    fun `with nothing on there are no tags`() {
        show(song)

        composeRule.onNodeWithText("SLEEP", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("LEVEL", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `the shuffle chip says whether it is on`() {
        show(song.copy(shuffle = true))

        composeRule.onNodeWithContentDescription("Shuffle").assertIsOn()
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
        openOption("Settings")

        assertEquals(listOf("speed 2.0", "sleep 30", "sleep at end", "sleep off", "settings"), done)
    }

    @Test
    fun `a live stream has no speed to set`() {
        show(song.copy(durationMs = null))

        composeRule.onNodeWithContentDescription("More options").performClick()

        composeRule.onNodeWithText("Speed 1x").assertDoesNotExist()
        composeRule.onNodeWithText("Sleep timer").assertIsDisplayed()
    }

    @Test
    fun `each key does its own thing`() {
        show(song)

        listOf(
            "Shuffle",
            "Previous. Hold to rewind 10 seconds",
            "Pause",
            "Next. Hold to skip 10 seconds",
            "Repeat is off",
            "Search",
            "Visual: Bars",
        ).forEach { composeRule.onNodeWithContentDescription(it).performClick() }

        assertEquals(listOf("shuffle", "previous", "toggle", "next", "repeat", "search", "visual"), done)
    }

    @Test
    fun `holding Previous or Next moves 10 seconds, also through TalkBack`() {
        show(song)

        listOf("Previous. Hold to rewind 10 seconds", "Next. Hold to skip 10 seconds").forEach { key ->
            val node = composeRule.onNodeWithContentDescription(key)
            val action = node.fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
            composeRule.runOnUiThread { action.action() }
        }

        assertEquals(listOf("seek by -10000", "seek by 10000"), done)
    }

    @Test
    fun `a move of the seek bar seeks once, to the place it was let go at`() {
        show(song)

        composeRule.onNodeWithContentDescription("Position in the song")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(120_000f) }

        assertEquals(listOf("seek 120000"), done)
    }

    @Test
    fun `a tap on the song opens its album`() {
        show(song.copy(albumId = 7))

        composeRule.onNodeWithContentDescription("Bhairavi by Ravi. Open the album Morning Ragas").performClick()

        assertEquals(listOf("album 7 Morning Ragas Ravi"), done)
    }

    @Test
    fun `a song with no album of the library has no link`() {
        show(song)

        composeRule.onNodeWithContentDescription("Bhairavi by Ravi").assert(hasClickAction().not())
    }

    @Test
    fun `the play mark and the repeat chip name what they show`() {
        show(song.copy(showPause = false, repeat = RepeatMode.ONE))

        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Repeat this song").assertIsDisplayed()
    }

    @Test
    fun `a station has no seek bar, no shuffle and no repeat, and says that it is live`() {
        show(song.copy(title = "Radio Mirchi", artist = "A song on air", album = null, durationMs = null))

        composeRule.onNodeWithText("Radio Mirchi", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Live").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Position in the song").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Shuffle").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Repeat is off").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun `an error says why, and the play mark tries again`() {
        show(song.copy(durationMs = null, showPause = false, error = PlaybackError.NETWORK))

        composeRule.onNodeWithText("No connection to the station. Check the network.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play").performClick()

        assertEquals(listOf("toggle"), done)
    }

    @Test
    fun `a station that waits for data says that it loads`() {
        show(song.copy(durationMs = null, isBuffering = true))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun `with nothing loaded the player offers to shuffle all songs or open the library`() {
        show(null)

        composeRule.onAllNodesWithText("Nothing playing").assertCountEquals(2)
        composeRule.onNodeWithText("The queue is empty").assertIsDisplayed()
        composeRule.onNodeWithText("Shuffle all songs").performClick()
        composeRule.onNodeWithText("Open library").performClick()
        openOption("Settings")

        assertEquals(listOf("shuffle all", "library", "settings"), done)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the screen passes the accessibility checks`() {
        show(song.copy(isFavourite = false, albumId = 7, speed = 1.5f), DisplayParts(levelling = true))

        composeRule.checkAccessibility()
    }

    @Test
    @Config(qualifiers = "+night")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the screen passes the accessibility checks in the dark look`() {
        show(song.copy(isFavourite = true, albumId = 7))

        composeRule.checkAccessibility()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the empty player passes the accessibility checks`() {
        show(null)

        composeRule.checkAccessibility()
    }
}
