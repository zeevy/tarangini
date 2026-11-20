/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The fake player is built on API that Media3 may still change.
@OptIn(UnstableApi::class)
// Moving the test clock by hand is an experimental coroutine API.
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionPlaybackQueueTest {
    private val connector = FakeSessionConnector()
    private val player = FakePlayer()
    private val link = PlayerLink(connector).also { link ->
        link.onScreenOpened()
        connector.accept(player)
    }

    private fun TestScope.queue(): SessionPlaybackQueue {
        val loader = QueueLoader(link, backgroundScope)
        return SessionPlaybackQueue(link, loader, backgroundScope)
    }

    /** Loads a, b, c and d, with b as the current item, playing. */
    private fun fourSongsSecondPlaying() = player.report {
        setPlaylist(listOf("a", "b", "c", "d").map(FakePlayer::queued))
            .setCurrentMediaItemIndex(1)
            .setPlaybackState(Player.STATE_READY)
            .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
    }

    private fun ids() = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId }

    @Test
    fun `play next puts the files right after the current item`() = runTest {
        fourSongsSecondPlaying()

        queue().playNext(listOf("x", "y"))
        runCurrent()

        assertEquals(listOf("a", "b", "x", "y", "c", "d"), ids())
        assertEquals("b", player.currentMediaItem?.mediaId)
    }

    @Test
    fun `add to end puts the files after the last item`() = runTest {
        fourSongsSecondPlaying()

        queue().addToEnd(listOf("x", "y"))
        runCurrent()

        assertEquals(listOf("a", "b", "c", "d", "x", "y"), ids())
        assertEquals("b", player.currentMediaItem?.mediaId)
    }

    @Test
    fun `play next and add to end with an empty queue start to play`() = runTest {
        queue().playNext(listOf("x"))
        runCurrent()
        assertEquals(listOf("x"), ids())
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `moving an item keeps the current item playing, also when it is the one moved`() = runTest {
        fourSongsSecondPlaying()
        val queue = queue()

        queue.move(from = 3, to = 0)
        runCurrent()
        assertEquals(listOf("d", "a", "b", "c"), ids())
        assertEquals("b", player.currentMediaItem?.mediaId)
        assertTrue(player.isPlaying)

        queue.move(from = 2, to = 3)
        runCurrent()
        assertEquals(listOf("d", "a", "c", "b"), ids())
        assertEquals("b", player.currentMediaItem?.mediaId)
        assertTrue(player.isPlaying)
    }

    @Test
    fun `an item of the queue plays from its start when it is picked, and a place that does not exist is ignored`() =
        runTest {
            fourSongsSecondPlaying()
            val queue = queue()

            queue.playAt(3)
            queue.playAt(4)
            queue.playAt(-1)
            runCurrent()

            assertEquals(listOf("seek to item 3 at 0", "play"), player.commands)
        }

    @Test
    fun `a move from or to a place that does not exist is ignored`() = runTest {
        fourSongsSecondPlaying()
        val queue = queue()

        queue.move(from = 4, to = 0)
        queue.move(from = 0, to = 4)
        queue.move(from = -1, to = 2)
        queue.move(from = 2, to = 2)
        runCurrent()

        assertTrue(player.commands.isEmpty())
    }

    @Test
    fun `removing an item that is not the current one keeps the current item`() = runTest {
        fourSongsSecondPlaying()

        queue().remove(0)
        runCurrent()

        assertEquals(listOf("b", "c", "d"), ids())
        assertEquals("b", player.currentMediaItem?.mediaId)
    }

    @Test
    fun `removing the current item makes the one after it current`() = runTest {
        fourSongsSecondPlaying()

        queue().remove(1)
        runCurrent()

        assertEquals(listOf("a", "c", "d"), ids())
        assertEquals("c", player.currentMediaItem?.mediaId)
    }

    @Test
    fun `removing a place that does not exist is ignored`() = runTest {
        fourSongsSecondPlaying()
        val queue = queue()

        queue.remove(4)
        queue.remove(-1)
        runCurrent()

        assertTrue(player.commands.isEmpty())
    }

    @Test
    fun `clear empties the queue and playback stops`() = runTest {
        fourSongsSecondPlaying()

        queue().clear()
        runCurrent()

        assertEquals(0, player.mediaItemCount)
        assertFalse(player.isPlaying)
    }

    @Test
    fun `clear also gives up a list that is still loading`() = runTest {
        val loader = QueueLoader(link, backgroundScope)
        val queue = SessionPlaybackQueue(link, loader, backgroundScope)
        player.afterNextAdd = { queue.clear() }

        loader.replace(List(20_000) { "song $it" }, startIndex = 0)
        runCurrent()

        assertEquals(0, player.mediaItemCount)
    }

    @Test
    fun `shuffle is switched on and off`() = runTest {
        val queue = queue()

        queue.setShuffle(true)
        runCurrent()
        assertTrue(player.shuffleModeEnabled)

        queue.setShuffle(false)
        runCurrent()
        assertFalse(player.shuffleModeEnabled)
    }

    @Test
    fun `each repeat mode reaches the player`() = runTest {
        val queue = queue()
        val expected = mapOf(
            RepeatMode.ONE to Player.REPEAT_MODE_ONE,
            RepeatMode.ALL to Player.REPEAT_MODE_ALL,
            RepeatMode.OFF to Player.REPEAT_MODE_OFF,
        )

        expected.forEach { (mode, playerMode) ->
            queue.setRepeat(mode)
            runCurrent()

            assertEquals(playerMode, player.repeatMode)
        }
    }
}
