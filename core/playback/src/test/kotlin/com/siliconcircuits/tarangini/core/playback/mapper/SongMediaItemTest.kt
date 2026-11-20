/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.mapper

import androidx.media3.common.MediaMetadata
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.playback.artwork.ArtworkAddress
import com.siliconcircuits.tarangini.core.playback.session.toPlayable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongMediaItemTest {
    private val song = Song(
        id = 3,
        mediaUri = "content://media/external/audio/media/42",
        title = "Bhairavi",
        artistId = 1,
        artist = "Ravi",
        albumId = 2,
        album = "Morning",
        durationMs = 240_000,
        trackNumber = 4,
        discNumber = 1,
        year = 1998,
        dateAdded = 0,
    )

    @Test
    fun `a song becomes a playable item with its address as id and its texts`() {
        val item = song.toMediaItem()

        assertEquals(song.mediaUri, item.mediaId)
        assertEquals(song.mediaUri, item.localConfiguration?.uri.toString())
        assertEquals("Bhairavi", item.mediaMetadata.title)
        assertEquals("Ravi", item.mediaMetadata.artist)
        assertEquals("Morning", item.mediaMetadata.albumTitle)
        assertEquals(4, item.mediaMetadata.trackNumber)
        assertEquals(240_000L, item.mediaMetadata.durationMs)
        assertEquals(ArtworkAddress.ofSong(song.mediaUri), item.mediaMetadata.artworkUri)
        assertEquals(MediaMetadata.MEDIA_TYPE_MUSIC, item.mediaMetadata.mediaType)
        assertEquals(true, item.mediaMetadata.isPlayable)
        assertEquals(false, item.mediaMetadata.isBrowsable)
    }

    @Test
    fun `a name the file does not give is left out`() {
        val item = song.copy(artist = "", album = "").toMediaItem()

        assertNull(item.mediaMetadata.artist)
        assertNull(item.mediaMetadata.albumTitle)
    }

    @Test
    fun `a length the phone did not give is left out`() {
        assertNull(song.copy(durationMs = 0).toMediaItem().mediaMetadata.durationMs)
    }

    @Test
    fun `an item made from an address alone asks for the cover picture of that song`() {
        val item = playableItem(song.mediaUri)

        assertEquals(song.mediaUri, item.mediaId)
        assertEquals(song.mediaUri, item.localConfiguration?.uri.toString())
        assertEquals(ArtworkAddress.ofSong(song.mediaUri), item.mediaMetadata.artworkUri)
        assertNull(item.mediaMetadata.title)
    }

    @Test
    fun `an item for something that is not a local song asks for no picture`() {
        assertNull(playableItem("https://example.org/stream").mediaMetadata.artworkUri)
    }

    @Test
    fun `the item passes the rule for what a caller may ask the player to play`() {
        assertEquals(song.mediaUri, song.toMediaItem().toPlayable().mediaId)
    }
}
