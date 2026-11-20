/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RequestedMediaItemTest {
    private val song = "content://media/external/audio/media/42"

    private fun requested(uri: String): MediaItem = MediaItem.Builder()
        .setMediaId("id")
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(uri)).build())
        .build()

    @Test
    fun `an audio item of MediaStore is accepted, asked for either way`() {
        assertEquals(Uri.parse(song), requested(song).toPlayable().localConfiguration?.uri)
        assertEquals(Uri.parse(song), MediaItem.fromUri(song).toPlayable().localConfiguration?.uri)
        assertEquals("id", requested(song).toPlayable().mediaId)
        assertEquals(
            "content://media/internal/audio/media/7",
            requested("content://media/internal/audio/media/7").toPlayable().localConfiguration?.uri.toString(),
        )
    }

    @Test
    fun `a file path, a web address and the content of another app are refused`() {
        val refused = listOf(
            "file:///data/data/com.siliconcircuits.tarangini/databases/tarangini.db",
            "/sdcard/Music/a.mp3",
            "https://example.org/a.mp3",
            "content://com.other.app.provider/secret",
            "content://media.evil/external/audio/media/42",
            "content://0@media/external/audio/media/42",
            "content://media:80/external/audio/media/42",
            "CONTENT://media/external/audio/media/42",
            "content:media/external/audio/media/42",
            "android.resource://com.siliconcircuits.tarangini/raw/a",
        )

        refused.forEach { uri ->
            assertThrows(uri, IllegalArgumentException::class.java) { requested(uri).toPlayable() }
        }
    }

    @Test
    fun `a MediaStore address that is not one audio item is refused`() {
        val refused = listOf(
            "content://media/external/images/media/42",
            "content://media/external/video/media/42",
            "content://media/external/file/42",
            "content://media/external/audio/media",
            "content://media/external/audio/media/42/extra",
            "content://media/external/audio/media/abc",
            "content://media/external/audio/albums/42",
        )

        refused.forEach { uri ->
            assertThrows(uri, IllegalArgumentException::class.java) { requested(uri).toPlayable() }
        }
    }

    @Test
    fun `an item that names no file is refused`() {
        assertThrows(IllegalArgumentException::class.java) { MediaItem.Builder().setMediaId("id").build().toPlayable() }
    }

    @Test
    fun `nothing else the caller sent reaches the player`() {
        val private = Uri.parse("file:///data/data/com.siliconcircuits.tarangini/files/secret")
        val loaded = MediaItem.Builder()
            .setMediaId("id")
            .setUri(song)
            .setSubtitleConfigurations(
                listOf(MediaItem.SubtitleConfiguration.Builder(private).setMimeType(MimeTypes.TEXT_VTT).build()),
            )
            .setAdsConfiguration(MediaItem.AdsConfiguration.Builder(Uri.parse("https://example.org/ads")).build())
            .setCustomCacheKey("key")
            .setMediaMetadata(MediaMetadata.Builder().setArtworkUri(Uri.parse("https://example.org/a.png")).build())
            .build()

        val playable = loaded.toPlayable()

        // The only picture address is the one this app makes from the checked address of the song.
        val ownArtwork = MediaMetadata.Builder().setArtworkUri(Uri.parse("$song/albumart")).build()
        assertEquals(MediaItem.Builder().setMediaId("id").setUri(song).setMediaMetadata(ownArtwork).build(), playable)
        assertTrue(requireNotNull(playable.localConfiguration).subtitleConfigurations.isEmpty())
        assertNull(playable.localConfiguration?.adsConfiguration)
    }
}
