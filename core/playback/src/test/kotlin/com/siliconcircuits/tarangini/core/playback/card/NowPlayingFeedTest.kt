/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.card

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCard
import com.siliconcircuits.tarangini.core.domain.playback.NowPlayingCards
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NowPlayingFeedTest {
    private val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
    private val scope: CoroutineScope = MainScope()
    private val shown = mutableListOf<NowPlayingCard?>()
    private val view = NowPlayingCards { card -> shown += card }

    private fun song(id: Int, title: String) = MediaItem.Builder()
        .setMediaId("content://media/external/audio/media/$id")
        .setUri("content://media/external/audio/media/$id")
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("Ravi").build())
        .build()

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    @After
    fun release() {
        player.release()
        scope.cancel()
    }

    @Test
    fun `a view gets the song and then play, and not the empty player at the start`() {
        player.addListener(NowPlayingFeed(player, setOf(view), scope))
        settle()

        player.setMediaItems(listOf(song(1, "Bhairavi")))
        settle()
        player.play()
        settle()

        val bhairavi = NowPlayingCard("Bhairavi", "Ravi", "content://media/external/audio/media/1", showsPause = false)
        assertEquals(listOf(bhairavi, bhairavi.copy(showsPause = true)), shown)
    }

    @Test
    fun `the next song is passed on, a seek sends nothing`() {
        player.setMediaItems(listOf(song(1, "Bhairavi"), song(2, "Yaman")))
        settle()
        player.addListener(NowPlayingFeed(player, setOf(view), scope))
        settle()

        player.seekTo(0, 30_000)
        settle()
        player.seekToNextMediaItem()
        settle()

        assertEquals(listOf("Yaman"), shown.map { it?.title })
    }

    @Test
    fun `a stream has no cover address`() {
        val address = "https://radio.example/live"
        val stream = MediaItem.Builder().setMediaId(address).setUri(address).build()
        player.setMediaItems(listOf(stream))
        settle()

        assertNull(cardOf(player)?.songUri)
    }

    @Test
    fun `at the end of the service the views show a play button`() {
        player.setMediaItems(listOf(song(1, "Bhairavi")))
        player.play()
        val feed = NowPlayingFeed(player, setOf(view), scope)
        settle()

        feed.end()
        settle()

        assertEquals(listOf(false), shown.map { it?.showsPause })
    }

    @Test
    fun `a view that fails does not stop the others`() {
        val broken = NowPlayingCards { error("The view is gone") }
        player.setMediaItems(listOf(song(1, "Bhairavi")))
        settle()

        player.addListener(NowPlayingFeed(player, linkedSetOf(broken, view), scope))
        settle()
        player.play()
        settle()

        assertEquals(listOf("Bhairavi"), shown.map { it?.title })
    }
}
