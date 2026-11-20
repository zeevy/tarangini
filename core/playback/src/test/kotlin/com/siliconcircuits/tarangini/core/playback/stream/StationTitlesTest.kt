/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.icy.IcyInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// The classes that carry ICY metadata are API of Media3 that may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class StationTitlesTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val titles = StationTitles(player)

    @After
    fun release() = player.release()

    private fun play(station: String?, address: String = "http://radio.example/stream") {
        val texts = MediaMetadata.Builder().setStation(station).build()
        player.setMediaItem(MediaItem.Builder().setMediaId("radio").setUri(address).setMediaMetadata(texts).build())
        // The player tells every listener when a new item has its texts.
        titles.onMediaMetadataChanged(player.mediaMetadata)
    }

    private fun sendTitle(title: String?) = titles.onMetadata(Metadata(IcyInfo(ByteArray(0), title, null)))

    private val shown: MediaMetadata get() = checkNotNull(player.currentMediaItem).mediaMetadata

    @Test
    fun `a station shows its name as the title before any song title comes`() {
        play("Radio Bhairavi")

        assertEquals("Radio Bhairavi", shown.title.toString())
        assertNull(shown.artist)
    }

    @Test
    fun `a song title from the station becomes the title, with the station under it`() {
        play("Radio Bhairavi")

        sendTitle("Ravi Shankar - Raga Bhairavi")

        assertEquals("Ravi Shankar - Raga Bhairavi", shown.title.toString())
        assertEquals("Radio Bhairavi", shown.artist.toString())
        assertEquals("radio", player.currentMediaItem?.mediaId)
        assertEquals("http://radio.example/stream", player.currentMediaItem?.localConfiguration?.uri.toString())
    }

    @Test
    fun `the next song replaces the title, and the same title again changes nothing`() {
        play("Radio Bhairavi")
        sendTitle("First song")

        sendTitle("Second song")
        val afterSecond = player.currentMediaItem
        sendTitle("Second song")
        titles.onMediaMetadataChanged(player.mediaMetadata)

        assertEquals("Second song", shown.title.toString())
        assertSame(afterSecond, player.currentMediaItem)
    }

    @Test
    fun `an empty title after a song brings the name of the station back`() {
        play("Radio Bhairavi")

        sendTitle("Song")
        sendTitle(" - ")

        assertEquals("Radio Bhairavi", shown.title.toString())
        assertNull(shown.artist)
    }

    @Test
    fun `a title of 5,000 characters is cut before it reaches the player`() {
        play("Radio Bhairavi")

        sendTitle("x".repeat(5_000))

        assertEquals(200, shown.title?.length)
    }

    @Test
    fun `a station with no name shows the host of its address, and never stays without a title`() {
        play(station = null)
        assertEquals("radio.example", shown.title.toString())

        sendTitle("Song")
        assertEquals("Song", shown.title.toString())
        assertEquals("radio.example", shown.artist.toString())

        sendTitle("")
        assertEquals("radio.example", shown.title.toString())
        assertNull(shown.artist)
    }

    @Test
    fun `a file of the phone keeps its own texts, whatever arrives`() {
        val texts = MediaMetadata.Builder().setTitle("Bhairavi").setArtist("Ravi Shankar").build()
        val song = MediaItem.Builder()
            .setMediaId("song")
            .setUri("content://media/external/audio/media/1")
            .setMediaMetadata(texts)
            .build()
        player.setMediaItem(song)

        titles.onMediaMetadataChanged(player.mediaMetadata)
        sendTitle("A title that came late from the station before")

        assertSame(song, player.currentMediaItem)
    }

    @Test
    fun `with nothing loaded a title changes nothing`() {
        sendTitle("Song")

        assertNull(player.currentMediaItem)
    }
}
