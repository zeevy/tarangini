/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.resume

import android.database.SQLException
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback
import com.siliconcircuits.tarangini.core.playback.controller.FakePlayer
import com.siliconcircuits.tarangini.core.testing.playback.FakeResumeStore
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackResumeTest {
    private val player = FakePlayer()
    private val store = FakeResumeStore()

    private val threeSongs = SavedPlayback(
        mediaIds = listOf("a", "b", "c"),
        place = ResumePlace(index = 1, positionMs = 42_000, shuffle = true, repeat = RepeatMode.ALL),
    )

    private fun TestScope.started(): PlaybackResume = PlaybackResume(player, store, backgroundScope).also {
        it.start()
        runCurrent()
    }

    private fun queue() = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId }

    private fun items(vararg ids: String) = ids.map { MediaItem.Builder().setMediaId(it).build() }

    @Test
    fun `the saved queue comes back at its place, and nothing is prepared or played`() = runTest {
        store.saved = threeSongs

        started()

        assertEquals(listOf("a", "b", "c"), queue())
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(42_000, player.currentPosition)
        assertTrue(player.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
        assertFalse(player.playWhenReady)
        assertFalse("prepare" in player.commands || "play" in player.commands)
    }

    @Test
    fun `the queue that comes back has the texts of the library, also for the request to go on`() = runTest {
        store.saved = threeSongs
        val withTexts: suspend (List<MediaItem>) -> List<MediaItem> = { items ->
            items.map { item ->
                val texts = MediaMetadata.Builder().setTitle("Title of ${item.mediaId}").build()
                item.buildUpon().setMediaMetadata(texts).build()
            }
        }

        val resume = PlaybackResume(player, store, backgroundScope, withTexts).also { it.start() }
        runCurrent()

        assertEquals("Title of b", player.getMediaItemAt(1).mediaMetadata.title)
        assertEquals(listOf("a", "b", "c"), queue())
        assertEquals("Title of c", resume.resumption()?.mediaItems?.get(2)?.mediaMetadata?.title)
    }

    @Test
    fun `the queue that came back is not written again, but a new place in it is`() = runTest {
        store.saved = threeSongs
        started()

        player.seekTo(2, 5_000)
        player.report()
        advanceTimeBy(2.seconds)

        assertEquals(0, store.queueSaves)
        assertEquals(2, store.saved?.place?.index)
        assertEquals(5_000L, store.saved?.place?.positionMs)
    }

    @Test
    fun `the empty player of a new service does not wipe what is stored`() = runTest {
        started()
        advanceTimeBy(10.seconds)

        assertEquals(0, store.queueSaves)
        assertNull(store.saved)
    }

    @Test
    fun `a new queue that comes in parts is saved once, when it is still`() = runTest {
        started()

        player.setMediaItems(items("a", "b"), 1, 0)
        player.report()
        advanceTimeBy(0.5.seconds)
        player.addMediaItems(items("c"))
        player.report()
        advanceTimeBy(3.seconds)

        assertEquals(1, store.queueSaves)
        assertEquals(listOf("a", "b", "c"), store.saved?.mediaIds)
        assertEquals(1, store.saved?.place?.index)
    }

    @Test
    fun `the place is saved every few seconds while something plays, and not while paused`() = runTest {
        store.saved = threeSongs
        started()
        player.prepare()
        player.play()
        player.report()

        player.positionMs = 50_000
        advanceTimeBy(6.seconds)
        assertEquals(50_000L, store.saved?.place?.positionMs)

        player.pause()
        player.report()
        runCurrent()
        player.positionMs = 99_000
        advanceTimeBy(20.seconds)
        assertEquals(50_000L, store.saved?.place?.positionMs)
    }

    @Test
    fun `the place is saved a last time when the service ends`() = runTest {
        store.saved = threeSongs
        val resume = started()
        player.positionMs = 77_000

        resume.end()
        runCurrent()

        assertEquals(77_000L, store.saved?.place?.positionMs)
    }

    @Test
    fun `a plain play on the queue that came back prepares the player`() = runTest {
        store.saved = threeSongs
        started()

        player.play()
        player.report()

        assertTrue("prepare" in player.commands)
    }

    @Test
    fun `a request to go on gets the saved queue and its place`() = runTest {
        store.saved = threeSongs

        val answer = requireNotNull(started().resumption())

        assertEquals(listOf("a", "b", "c"), answer.mediaItems.map { it.mediaId })
        assertEquals("a", answer.mediaItems.first().localConfiguration?.uri.toString())
        assertEquals(1, answer.startIndex)
        assertEquals(42_000L, answer.startPositionMs)
    }

    @Test
    fun `a request to go on gets nothing when nothing was saved`() = runTest {
        assertNull(started().resumption())
    }

    @Test
    fun `a request to go on does not bring back a queue that was emptied since the start`() = runTest {
        store.saved = threeSongs
        val resume = started()

        store.saved = null

        assertNull(resume.resumption())
    }

    @Test
    fun `a change of the queue just before the service ends is saved`() = runTest {
        store.saved = threeSongs
        val resume = started()

        player.addMediaItems(items("d"))
        player.report()
        resume.end()
        runCurrent()

        assertEquals(listOf("a", "b", "c", "d"), store.saved?.mediaIds)
    }

    @Test
    fun `a player that was filled before the saved queue was read keeps its queue, and that queue is saved`() =
        runTest {
            store.saved = threeSongs
            player.setMediaItems(items("x", "y"), 1, 0)

            started()
            advanceTimeBy(2.seconds)

            assertEquals(listOf("x", "y"), queue())
            assertEquals(listOf("x", "y"), store.saved?.mediaIds)
            assertEquals(1, store.saved?.place?.index)
        }

    @Test
    fun `a database that fails does not stop the service, and the next save works`() = runTest {
        store.failure = SQLException("disk")
        val resume = started()
        assertNull(resume.resumption())
        player.setMediaItems(items("a"), 0, 0)
        player.report()
        advanceTimeBy(2.seconds)
        assertNull(store.saved)

        store.failure = null
        player.addMediaItems(items("b"))
        player.report()
        advanceTimeBy(2.seconds)

        assertEquals(listOf("a", "b"), store.saved?.mediaIds)
    }
}
