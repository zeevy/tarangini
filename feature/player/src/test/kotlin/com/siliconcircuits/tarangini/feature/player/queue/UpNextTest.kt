/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.testing.RegisterComponentActivityRule
import com.siliconcircuits.tarangini.core.testing.savedPicture
import com.siliconcircuits.tarangini.core.ui.theme.TaranginiTheme
import com.siliconcircuits.tarangini.feature.player.checkAccessibility
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingActions
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingContent
import com.siliconcircuits.tarangini.feature.player.nowplaying.NowPlayingUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class UpNextTest {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(RegisterComponentActivityRule()).around(composeRule)

    private val done = mutableListOf<String>()
    private val song = NowPlayingUiState(
        title = "Evening raga",
        artist = "Zakir Hussain",
        album = "Ragas",
        artworkUri = null,
        showPause = true,
        isBuffering = false,
        positionMs = 20_000,
        durationMs = 245_000,
        shuffle = false,
        repeat = RepeatMode.OFF,
        isFavourite = false,
        place = 2,
        queueSize = 4,
    )
    private val short = QueueUiState(
        items = listOf(
            PlaybackItem("a", "Morning raga", "Ravi Shankar", durationMs = 185_000),
            PlaybackItem("b", "Evening raga", "Zakir Hussain", durationMs = 245_000),
            PlaybackItem(
                "c",
                "A very long title of a song that does not fit on one line",
                "Hariprasad Chaurasia",
                durationMs = 1_265_000,
            ),
            PlaybackItem("d", "Night raga", null, durationMs = 62_000),
        ),
        currentIndex = 1,
    )
    private val long = QueueUiState(
        items = List(LONG_QUEUE) {
            PlaybackItem(
                "song$it",
                "Song ${it + 1}",
                "Artist ${it % 7}",
                durationMs =
                    200_000L + it,
            )
        },
        currentIndex = 2,
    )

    private fun show(queue: QueueUiState, now: NowPlayingUiState = song) = composeRule.setContent {
        TaranginiTheme {
            NowPlayingContent(
                now,
                NowPlayingActions({}, {}, {}, {}, {}, {}),
                onBackClick = {},
                onOpenQueue = { done += "edit" },
                below = { upNext(queue, 560.dp, { done += "play $it" }, { done += "edit" }) },
            )
        }
    }

    private fun scrollTo(description: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription(description, substring = true))
    }

    @Test
    fun `the list starts at the song that plays and says how many are left`() {
        show(short)
        scrollTo("Number 4")

        composeRule.onNodeWithText("Up next").assertExists()
        composeRule.onNodeWithText("2 songs left").assertExists()
        composeRule.onNodeWithContentDescription(
            "Number 2, Evening raga, Zakir Hussain, 4:05, Playing now",
        ).assertExists()
        composeRule.onNodeWithContentDescription("Number 4, Night raga, 1:02").assertExists()
        // The song already played is on the queue screen, not here.
        composeRule.onAllNodesWithText("Morning raga", substring = true).assertCountEquals(0)
    }

    @Test
    fun `a tap on a row plays that song and Edit opens the queue`() {
        show(short)
        scrollTo("Number 3")

        composeRule.onNodeWithContentDescription("Number 3", substring = true).performClick()
        composeRule.onNodeWithText("Edit").performClick()

        assertEquals(listOf("play 2", "edit"), done)
    }

    @Test
    fun `the last song says nothing comes after it`() {
        show(short.copy(currentIndex = 3))
        composeRule.onNode(
            hasScrollAction(),
        ).performScrollToNode(androidx.compose.ui.test.hasText("Nothing after this song"))

        composeRule.onNodeWithText("0 songs left").assertExists()
    }

    @Test
    fun `with shuffle on the rows follow the order the player plays them`() {
        show(short.copy(currentIndex = 1, playOrder = listOf(1, 3, 0)))
        scrollTo("Number 1,")

        composeRule.onNodeWithText("2 songs left").assertExists()
        val spoken = composeRule.onAllNodes(hasContentDescription("Number", substring = true) and hasClickAction())
            .fetchSemanticsNodes().map {
                it.config[SemanticsProperties.ContentDescription].first().substringBefore(",")
            }
        assertEquals(listOf("Number 2", "Number 4", "Number 1"), spoken)
    }

    @Test
    fun `a stream has no up next list`() {
        show(QueueUiState(listOf(PlaybackItem("http://radio", "Radio", isStream = true)), 0))

        composeRule.onAllNodesWithText("Up next").assertCountEquals(0)
    }

    @Test
    fun `a long queue draws only the rows in view`() {
        show(long)
        scrollTo("Number 3")

        composeRule.onNodeWithText("$LONG_LEFT songs left").assertExists()
        composeRule.onAllNodesWithText("Song $LONG_QUEUE", substring = true).assertCountEquals(0)
        scrollTo("Number 40,")
        composeRule.onNode(hasContentDescription("Number 40,", substring = true) and hasClickAction()).assertExists()
    }

    @Test
    fun `the list passes the accessibility checks`() {
        show(short)
        scrollTo("Number 4")

        composeRule.checkAccessibility()
    }

    @Test
    @Config(qualifiers = "+night")
    fun `the list passes the accessibility checks in the dark look`() {
        show(short)
        scrollTo("Number 4")

        composeRule.checkAccessibility()
    }

    @Test
    fun `a short queue`() = shot("up_next_short") {
        show(short)
        scrollTo("Number 4")
    }

    @Test
    @Config(qualifiers = "+night")
    fun `a short queue in the dark look`() = shot("up_next_short_dark") {
        show(short)
        scrollTo("Number 4")
    }

    @Test
    fun `a long queue`() = shot("up_next_long") {
        show(long)
        scrollTo("Number 12,")
    }

    @Test
    @Config(qualifiers = "+night")
    fun `a long queue in the dark look`() = shot("up_next_long_dark") {
        show(long)
        scrollTo("Number 12,")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun `a short queue with the largest font`() {
        RuntimeEnvironment.setFontScale(LARGEST_FONT)
        shot("up_next_largest_font") {
            show(short)
            scrollTo("Number 4")
        }
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun `a short queue on a wide screen`() = shot("up_next_wide") {
        show(short)
        scrollTo("Number 4")
    }

    @Test
    fun `the end of the queue`() = shot("up_next_end") {
        show(short.copy(currentIndex = 3))
        composeRule.onNode(
            hasScrollAction(),
        ).performScrollToNode(androidx.compose.ui.test.hasText("Nothing after this song"))
    }

    private fun shot(name: String, content: () -> Unit) {
        val picture = savedPicture(name)
        content()
        composeRule.onRoot().captureRoboImage(picture.path)
    }

    private companion object {
        const val LARGEST_FONT = 2f
        const val LONG_QUEUE = 2_000
        const val LONG_LEFT = LONG_QUEUE - 3
    }
}
