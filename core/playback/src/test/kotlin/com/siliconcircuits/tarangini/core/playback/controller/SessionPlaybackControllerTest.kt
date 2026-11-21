/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

// The fake player is built on API that Media3 may still change.
@OptIn(UnstableApi::class)
// Moving the test clock by hand is an experimental coroutine API.
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionPlaybackControllerTest {
    private val connector = FakeSessionConnector()
    private val player = FakePlayer()

    private val link = PlayerLink(connector)

    private fun TestScope.controller() =
        SessionPlaybackController(link, QueueLoader(link, backgroundScope), backgroundScope)

    private fun TestScope.connectedController(): SessionPlaybackController = controller().also { controller ->
        link.onScreenOpened()
        connector.accept(player)
    }

    private fun FakePlayer.playTwoSongs() = report {
        setPlaylist(listOf(FakePlayer.queued("first"), FakePlayer.queued("second")))
            .setPlaybackState(Player.STATE_READY)
            .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
    }

    @Test
    fun `nothing connects before a screen is open`() = runTest {
        val controller = controller()

        assertEquals(0, connector.attemptCount)
        assertEquals(PlaybackState(), controller.state.value)
    }

    @Test
    fun `the first screen opens one connection and a second screen shares it`() = runTest {
        controller()

        link.onScreenOpened()
        link.onScreenOpened()

        assertEquals(1, connector.attemptCount)
    }

    @Test
    fun `the state shows what the player does from the moment it connects`() = runTest {
        player.playTwoSongs()

        val controller = connectedController()

        assertTrue(controller.state.value.isPlaying)
        assertEquals("first", controller.state.value.currentItem?.id)
    }

    @Test
    fun `an event of the player changes the state`() = runTest {
        val controller = connectedController()

        player.playTwoSongs()
        assertTrue(controller.state.value.isPlaying)

        player.report {
            setCurrentMediaItemIndex(1).setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        }
        assertFalse(controller.state.value.isPlaying)
        assertEquals("second", controller.state.value.currentItem?.id)
    }

    @Test
    fun `the position moves while the player plays and stands still while it is paused`() = runTest {
        val controller = connectedController()
        backgroundScope.launch { controller.state.collect {} }
        player.playTwoSongs()

        player.positionMs = 1_700
        assertEquals(0L, controller.state.value.positionMs)
        advanceTimeBy(600)
        assertEquals(1_700L, controller.state.value.positionMs)

        player.report { setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
        player.positionMs = 9_000
        advanceTimeBy(2_000)
        assertEquals(1_700L, controller.state.value.positionMs)
    }

    @Test
    fun `the position is not read while nobody watches the state`() = runTest {
        val controller = connectedController()
        player.playTwoSongs()

        player.positionMs = 5_000
        advanceTimeBy(2_000)
        assertEquals(0L, controller.state.value.positionMs)

        backgroundScope.launch { controller.state.collect {} }
        runCurrent()
        assertEquals(5_000L, controller.state.value.positionMs)
    }

    @Test
    fun `the queue is read again only when it may have changed`() = runTest {
        val controller = connectedController()
        player.playTwoSongs()
        val queue = controller.state.value.queue

        player.report { setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
        assertTrue(queue === controller.state.value.queue)

        player.report { setPlaylist(listOf(FakePlayer.queued("third"))) }
        assertEquals(listOf("third"), controller.state.value.queue.map { it.id })
    }

    @Test
    fun `play loads the files and starts from the asked place`() = runTest {
        val controller = connectedController()

        controller.play(listOf("content://media/external/audio/media/1", "content://media/external/audio/media/2"), 1)
        runCurrent()

        assertEquals(
            listOf(
                "load [content://media/external/audio/media/1, content://media/external/audio/media/2] from 1",
                "prepare",
                "play",
            ),
            player.commands,
        )
        player.report()
        assertEquals(1, controller.state.value.currentIndex)
        assertEquals("content://media/external/audio/media/2", controller.state.value.currentItem?.id)
    }

    @Test
    fun `play with a start place outside the list starts at the nearest item, and an empty list does nothing`() =
        runTest {
            val controller = connectedController()

            controller.play(emptyList())
            controller.play(listOf("content://media/external/audio/media/1"), startIndex = 7)
            runCurrent()

            assertEquals("load [content://media/external/audio/media/1] from 0", player.commands.first())
        }

    @Test
    fun `pause, resume, seek, next and previous reach the player`() = runTest {
        val controller = connectedController()
        player.playTwoSongs()

        controller.pause()
        controller.resume()
        controller.seekTo(30_000)
        controller.next()
        controller.previous()
        runCurrent()

        assertEquals(
            listOf("pause", "play", "seek to item 0 at 30000", "seek to item 1 at 0", "seek to item 0 at 0"),
            player.commands,
        )
    }

    @Test
    fun `resume prepares a player that stopped after an error`() = runTest {
        val controller = connectedController()
        player.report { setPlaylist(listOf(FakePlayer.queued("first"))).setPlaybackState(Player.STATE_IDLE) }

        controller.resume()
        runCurrent()

        assertEquals(listOf("prepare", "play"), player.commands)
    }

    @Test
    fun `resume starts the item again at the end of the queue`() = runTest {
        val controller = connectedController()
        player.report { setPlaylist(listOf(FakePlayer.queued("first"))).setPlaybackState(Player.STATE_ENDED) }

        controller.resume()
        runCurrent()

        assertEquals(listOf("seek to item 0 at 0", "play"), player.commands)
    }

    @Test
    fun `a command sent before the connection is made runs when it is made`() = runTest {
        val controller = controller()
        link.onScreenOpened()

        controller.pause()
        runCurrent()
        assertTrue(player.commands.isEmpty())

        connector.accept(player)

        assertEquals(listOf("pause"), player.commands)
    }

    @Test
    fun `a command with no screen open is ignored and opens no connection`() = runTest {
        val controller = controller()

        controller.pause()
        runCurrent()
        assertEquals(0, connector.attemptCount)

        link.onScreenOpened()
        connector.accept(player)
        assertTrue(player.commands.isEmpty())
    }

    @Test
    fun `the state is still right for a screen that is built again after a rotation`() = runTest {
        val controller = connectedController()
        player.playTwoSongs()

        link.onScreenClosed(comesBack = true)
        link.onScreenOpened()

        assertEquals(1, connector.attemptCount)
        assertFalse(connector.lastReleased)
        assertTrue(controller.state.value.isPlaying)
        assertEquals("first", controller.state.value.currentItem?.id)
    }

    @Test
    fun `closing the last screen releases the connection and empties the state`() = runTest {
        val controller = connectedController()
        player.playTwoSongs()
        link.onScreenOpened()

        link.onScreenClosed(comesBack = false)
        assertFalse(connector.lastReleased)

        link.onScreenClosed(comesBack = false)
        assertTrue(connector.lastReleased)
        assertEquals(PlaybackState(), controller.state.value)

        player.report { setCurrentMediaItemIndex(1) }
        assertEquals(PlaybackState(), controller.state.value)
    }

    @Test
    fun `opening a screen again after the last one closed connects again`() = runTest {
        connectedController()
        link.onScreenClosed(comesBack = false)

        link.onScreenOpened()

        assertEquals(2, connector.attemptCount)
    }

    @Test
    fun `when the service restarts the connection is made again and the state shows the new player`() = runTest {
        val controller = connectedController()
        player.playTwoSongs()

        connector.serviceStops()

        assertEquals(PlaybackState(), controller.state.value)
        assertEquals(2, connector.attemptCount)

        val newPlayer = FakePlayer()
        newPlayer.report { setPlaylist(listOf(FakePlayer.queued("after restart"))) }
        connector.accept(newPlayer)

        assertEquals("after restart", controller.state.value.currentItem?.id)

        player.report { setCurrentMediaItemIndex(1) }
        assertEquals("after restart", controller.state.value.currentItem?.id)
    }

    @Test
    fun `a refused connection is not tried again by itself, the next command tries again`() = runTest {
        val controller = controller()
        link.onScreenOpened()
        controller.pause()
        runCurrent()

        connector.refuse()
        assertEquals(1, connector.attemptCount)

        controller.resume()
        runCurrent()
        assertEquals(2, connector.attemptCount)

        player.report { setPlaylist(listOf(FakePlayer.queued("first"))) }
        connector.accept(player)
        assertEquals(listOf("prepare", "play"), player.commands)
    }

    @Test
    fun `a late callback of an old connection changes nothing`() = runTest {
        val controller = connectedController()
        connector.serviceStops()
        val newPlayer = FakePlayer()
        connector.accept(newPlayer)

        connector.lateDisconnectOf(attemptIndex = 0)

        assertEquals(2, connector.attemptCount)
        assertFalse(connector.lastReleased)
        newPlayer.report { setPlaylist(listOf(FakePlayer.queued("still here"))) }
        assertEquals("still here", controller.state.value.currentItem?.id)
    }

    @Test
    fun `a speed is set with the pitch kept, held between half and double, and shows in the state`() = runTest {
        val controller = connectedController()
        player.playTwoSongs()

        controller.setSpeed(1.5f)
        controller.setSpeed(9f)
        controller.setSpeed(0.1f)
        // The commands are sent as their own piece of work; then the player tells its new state.
        runCurrent()
        player.report()

        assertEquals(listOf("speed 1.5 pitch 1.0", "speed 2.0 pitch 1.0", "speed 0.5 pitch 1.0"), player.commands)
        assertEquals(0.5f, controller.state.value.speed)
    }
}
