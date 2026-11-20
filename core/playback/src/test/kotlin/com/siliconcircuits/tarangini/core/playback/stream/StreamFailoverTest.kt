/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.siliconcircuits.tarangini.core.testing.station.FakeStationStreams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** A player that never opens an address, counts what it is asked and says that it has failed. */
// A player that wraps another one is API of Media3 that may still change.
@OptIn(UnstableApi::class)
private class FailedPlayer(player: ExoPlayer) : ForwardingPlayer(player) {
    var prepares = 0
    var seeksToStart = 0
    var error: PlaybackException? = PlaybackException("failed", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)

    override fun prepare() {
        prepares++
    }

    override fun seekToDefaultPosition() {
        seeksToStart++
    }

    override fun getPlayerError(): PlaybackException? = error
}

@RunWith(RobolectricTestRunner::class)
class StreamFailoverTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val real: ExoPlayer = ExoPlayer.Builder(context).build()

    private val player = FailedPlayer(real)
    private val stations = FakeStationStreams(
        listOf(listOf("https://a.example/1", "file:///sdcard/a.mp3", "https://a.example/2", "https://a.example/3")),
    )
    private val failover = StreamFailover(player, stations, CoroutineScope(Dispatchers.Unconfined))

    @After
    fun release() = real.release()

    private fun play(address: String) {
        val texts = MediaMetadata.Builder().setTitle("Radio").build()
        real.setMediaItem(MediaItem.Builder().setMediaId("station").setUri(address).setMediaMetadata(texts).build())
    }

    private fun fail(code: Int = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) =
        failover.onPlayerError(PlaybackException("failed", null, code))

    private val address: String get() = real.currentMediaItem?.localConfiguration?.uri.toString()

    @Test
    fun `a failed address gives way to the next one of the station, with the same id and texts`() {
        play("https://a.example/1")

        fail()

        assertEquals("https://a.example/2", address)
        assertEquals("station", real.currentMediaItem?.mediaId)
        assertEquals("Radio", real.currentMediaItem?.mediaMetadata?.title.toString())
        assertEquals(1, player.prepares)
    }

    @Test
    fun `each address is tried once, then the error stays, and the next failure starts a new round`() {
        play("https://a.example/2")

        fail()
        assertEquals("https://a.example/1", address)
        fail()
        assertEquals("https://a.example/3", address)
        fail()
        assertEquals("https://a.example/3", address)
        assertEquals(2, player.prepares)

        fail()
        assertEquals("https://a.example/1", address)
    }

    @Test
    fun `an address that played is tried again at the next failure`() {
        play("https://a.example/1")
        fail()
        failover.onIsPlayingChanged(true)

        fail()

        assertEquals("https://a.example/1", address)
    }

    @Test
    fun `an address of no stored station, a file of the phone and a failed read change nothing`() {
        play("https://unknown.example/")
        val unknown = real.currentMediaItem
        fail()
        assertSame(unknown, real.currentMediaItem)

        play("content://media/external/audio/media/1")
        val file = real.currentMediaItem
        fail()
        assertSame(file, real.currentMediaItem)

        stations.failure = IllegalStateException("no database")
        play("https://a.example/1")
        val station = real.currentMediaItem
        fail()
        assertSame(station, real.currentMediaItem)
    }

    @Test
    fun `an item whose texts changed since the failure still moves to the next address, with the new texts`() {
        play("https://a.example/1")
        val item = checkNotNull(real.currentMediaItem)
        stations.onRead = {
            val texts = MediaMetadata.Builder().setTitle("A song").build()
            real.replaceMediaItem(0, item.buildUpon().setMediaMetadata(texts).build())
        }

        fail()

        assertEquals("https://a.example/2", address)
        assertEquals("A song", real.currentMediaItem?.mediaMetadata?.title.toString())
    }

    @Test
    fun `nothing changes when the person tried again before the read was done`() {
        player.error = null
        play("https://a.example/1")
        val item = real.currentMediaItem

        fail()

        assertSame(item, real.currentMediaItem)
    }

    @Test
    fun `a stream that fell behind its live window starts again at the same address`() {
        play("https://a.example/1")
        val item = real.currentMediaItem

        fail(PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW)

        assertSame(item, real.currentMediaItem)
        assertEquals(1, player.seeksToStart)
        assertEquals(1, player.prepares)
    }
}
