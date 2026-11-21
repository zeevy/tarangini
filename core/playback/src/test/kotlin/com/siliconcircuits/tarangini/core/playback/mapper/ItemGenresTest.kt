/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.mapper

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.siliconcircuits.tarangini.core.testing.library.FakeGenreRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSongLookup
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.station.FakeStationStreams
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemGenresTest {
    private val uri = "content://media/external/audio/media/7"
    private val song = testSong(7, title = "Bhairavi").copy(mediaUri = uri)
    private val item = MediaItem.Builder().setMediaId(uri).setUri(uri).build()

    @Test
    fun `a queue item gets the genre keys of its song from the library`() = runTest {
        val genres = FakeGenreRepository().apply {
            songsByGenre =
                mapOf("carnatic" to listOf(song), "folk" to listOf(song))
        }

        val added = LibraryTexts(
            FakeSongLookup(listOf(song)),
            FakeStationStreams(),
            genres,
        ).addTo(listOf(item)).single()

        assertEquals(listOf("carnatic", "folk"), added.genreKeys())
        assertEquals("Bhairavi", added.mediaMetadata.title)
    }

    @Test
    fun `a song with no genre, and an item the library does not know, have no genre keys`() = runTest {
        val other = MediaItem.Builder().setMediaId("x").setUri("https://radio.example/live").build()

        val added = LibraryTexts(
            FakeSongLookup(listOf(song)),
            FakeStationStreams(),
            FakeGenreRepository(),
        ).addTo(listOf(item, other))

        assertEquals(listOf(emptyList<String>(), emptyList()), added.map { it.genreKeys() })
    }

    @Test
    fun `the other extras of an item stay`() {
        val extras = Bundle().apply { putString("other", "value") }

        val metadata = MediaMetadata.Builder().setExtras(extras).build().withGenreKeys(listOf("rock"))

        assertEquals("value", metadata.extras?.getString("other"))
        assertEquals(listOf("rock"), MediaItem.Builder().setMediaMetadata(metadata).build().genreKeys())
    }
}
