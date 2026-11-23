/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// The fake player is built on API that Media3 may still change.
@OptIn(UnstableApi::class)
// Moving the test clock by hand is an experimental coroutine API.
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class QueueLoaderTest {
    private val connector = FakeSessionConnector()
    private val player = FakePlayer()
    private val link = PlayerLink(connector).also { link ->
        link.onScreenOpened()
        connector.accept(player)
    }

    private fun TestScope.loader() = QueueLoader(link, backgroundScope)

    private fun songs(count: Int) = List(count) { "song $it" }

    private fun queue() = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId }

    @Test
    fun `a list started from an album names it, and a song added by hand takes the name away`() = runTest {
        val loader = loader()
        val album = QueueSource(QueueSourceKind.ALBUM, "Morning Ragas")

        loader.replace(songs(3), startIndex = 0, source = album)
        runCurrent()
        assertEquals(album, player.toPlaybackState().source)

        loader.add(listOf("one more")) { it.mediaItemCount }
        runCurrent()
        assertNull(player.toPlaybackState().source)
    }

    @Test
    fun `a list started with no source takes away the name of the list before it`() = runTest {
        val loader = loader()
        loader.replace(songs(2), startIndex = 0, source = QueueSource(QueueSourceKind.PLAYLIST, "Practice"))
        runCurrent()

        loader.replace(songs(2), startIndex = 1)
        runCurrent()

        assertNull(player.toPlaybackState().source)
    }

    @Test
    fun `a very long list plays its first items before the rest is loaded`() = runTest {
        loader().replace(songs(20_000), startIndex = 0)
        runCurrent()

        assertEquals("load ${songs(20)} from 0", player.commands[0])
        assertEquals(listOf("prepare", "play"), player.commands.subList(1, 3))
        assertTrue(player.commands.drop(3).all { it.startsWith("add ") })
        assertEquals(songs(20_000), queue())
        assertEquals(0, player.currentMediaItemIndex)
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `a long list started in the middle ends in its own order with the asked item current`() = runTest {
        loader().replace(songs(12_345), startIndex = 6_000)
        runCurrent()

        assertEquals(songs(12_345), queue())
        assertEquals(6_000, player.currentMediaItemIndex)
        assertEquals(4, player.commands.count { it.startsWith("add ") })
    }

    @Test
    fun `the rest of a list waits a short time for a player that is held back, then loads anyway`() = runTest {
        player.heldBack = true

        loader().replace(songs(100), startIndex = 0)
        runCurrent()
        assertEquals(20, player.mediaItemCount)

        advanceTimeBy(2_900)
        assertEquals(20, player.mediaItemCount)

        advanceTimeBy(200)
        assertEquals(songs(100), queue())
    }

    @Test
    fun `a new list gives up a list that is still loading`() = runTest {
        val loader = loader()
        player.afterNextAdd = { loader.replace(listOf("only one"), startIndex = 0) }

        loader.replace(songs(20_000), startIndex = 0)
        runCurrent()

        assertEquals(listOf("only one"), queue())
    }

    @Test
    fun `files added while a list is loading come after that list`() = runTest {
        val loader = loader()
        player.afterNextAdd = { loader.add(listOf("last")) { it.mediaItemCount } }

        loader.replace(songs(3_000), startIndex = 0)
        runCurrent()

        assertEquals(songs(3_000) + "last", queue())
    }

    @Test
    fun `cancel stops a load and keeps what is already in the player`() = runTest {
        val loader = loader()
        player.afterNextAdd = { loader.cancel() }

        loader.replace(songs(20_000), startIndex = 0)
        runCurrent()

        assertEquals(songs(5_020), queue())
    }

    @Test
    fun `a new list also gives up an older load that an added file was waiting for`() = runTest {
        val loader = loader()
        player.afterNextAdd = {
            loader.add(listOf("added in between")) { it.mediaItemCount }
            loader.replace(listOf("only one"), startIndex = 0)
        }

        loader.replace(songs(20_000), startIndex = 0)
        runCurrent()

        assertEquals(listOf("only one"), queue())
    }

    @Test
    fun `cancel also gives up an older load that an added file was waiting for`() = runTest {
        val loader = loader()
        player.afterNextAdd = {
            loader.add(listOf("added in between")) { it.mediaItemCount }
            loader.cancel()
        }

        loader.replace(songs(20_000), startIndex = 0)
        runCurrent()

        assertEquals(songs(5_020), queue())
    }

    @Test
    fun `a short list goes into the player in one call`() = runTest {
        loader().replace(songs(20), startIndex = 7)
        runCurrent()

        assertEquals(listOf("load ${songs(20)} from 7", "prepare", "play"), player.commands)
    }

    @Test
    fun `files added to an empty queue start to play, files added to a queue that is there do not`() = runTest {
        val loader = loader()

        loader.add(listOf("first")) { 0 }
        runCurrent()
        assertEquals(listOf("add [first] at 0", "prepare", "play"), player.commands)

        player.report { setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
        player.commands.clear()
        loader.add(listOf("second")) { it.mediaItemCount }
        runCurrent()
        assertEquals(listOf("add [second] at 1"), player.commands)
    }
}
