/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.playback.browse.BrowseItems
import com.siliconcircuits.tarangini.core.playback.browse.BrowseLabels
import com.siliconcircuits.tarangini.core.playback.browse.BrowseTree
import com.siliconcircuits.tarangini.core.playback.browse.CarSearch
import com.siliconcircuits.tarangini.core.playback.browse.MediaId
import com.siliconcircuits.tarangini.core.playback.mapper.playableItem
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryLetters
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSearchRepository
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
import com.siliconcircuits.tarangini.core.testing.library.testSong
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the car search shows, and what a spoken "play ..." starts. */
@RunWith(RobolectricTestRunner::class)
class CarPlayTest {
    private val library = FakeLibraryRepository()
    private val found = FakeSearchRepository()
    private val told = mutableListOf<NothingToPlay>()
    private var saved: MediaItemsWithStartPosition? = null
    private val items = BrowseItems(BrowseLabels("Library", "Songs", "Albums", "Artists", "Folders", "?", "?", null))
    private val search = CarSearch(found, library, items)
    private val tree = BrowseTree(library, FakeLibraryLetters(library), items)
    private val carPlay = CarPlay(MainScope(), tree, search, library, { saved }) { told += it }

    private fun spoken(words: String, extras: Bundle? = null): MediaItem {
        val request = MediaItem.RequestMetadata.Builder().setSearchQuery(words).setExtras(extras).build()
        return MediaItem.Builder().setRequestMetadata(request).build()
    }

    private fun played(item: MediaItem): List<String> =
        requireNotNull(carPlay.queueFor(item)).get().mediaItems.map { it.mediaId }

    private fun named(focus: String, key: String, name: String) = Bundle().apply {
        putString(MediaStore.EXTRA_MEDIA_FOCUS, focus)
        putString(key, name)
    }

    @Test
    fun `the search of the car gives songs, then artists, then albums`() = runBlocking {
        found.results = SearchResults(
            songs = listOf(testSong(1, "Bhairavi", albumId = 7)),
            albums = listOf(testAlbum(7, "Morning Ragas")),
            artists = listOf(testArtist(3, "Ravi Shankar")),
        )

        val ids = search.results("rag").map { it.mediaId }

        val expected = listOf(MediaId.SongIn(1, MediaId.Album(7)), MediaId.Artist(3), MediaId.Album(7))
        assertEquals(expected.map { it.encode() }, ids)
    }

    @Test
    fun `a text with no letter or digit finds nothing and is not searched`() = runBlocking {
        found.results = SearchResults(songs = listOf(testSong(1)))

        assertTrue(search.results("\"; -- ***").isEmpty())
        assertTrue(found.searched.isEmpty())
    }

    @Test
    fun `a very long text is cut before it reaches the search`() = runBlocking {
        search.results("a".repeat(5_000))

        assertEquals(200, found.searched.single().length)
    }

    @Test
    fun `a spoken song plays first, and the other songs found follow`() {
        found.results = SearchResults(songs = listOf(testSong(1, "Morning raga"), testSong(2, "Bhairavi")))

        assertEquals(listOf(testSong(2).mediaUri, testSong(1).mediaUri), played(spoken("Bhairavi!")))
        assertTrue(told.isEmpty())
    }

    @Test
    fun `a spoken artist plays the songs of the artist`() {
        library.songs = listOf(testSong(1, artistId = 3), testSong(2, artistId = 3), testSong(9, artistId = 4))
        found.results = SearchResults(artists = listOf(testArtist(3, "Ravi Shankar")))

        assertEquals(listOf(testSong(1).mediaUri, testSong(2).mediaUri), played(spoken("ravi shankar")))
    }

    @Test
    fun `a spoken album plays the songs of the album`() {
        library.songs = listOf(testSong(1, albumId = 7), testSong(2, albumId = 8))
        found.results = SearchResults(albums = listOf(testAlbum(7, "Morning Ragas")))

        assertEquals(listOf(testSong(1).mediaUri), played(spoken("morning ragas")))
    }

    @Test
    fun `a song title wins over an artist, unless the assistant says an artist was named`() {
        library.songs = listOf(testSong(5, artistId = 3))
        found.results = SearchResults(
            songs = listOf(testSong(1, "Ravi")),
            artists = listOf(testArtist(3, "Ravi Shankar")),
        )
        val artist = named(MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE, MediaStore.EXTRA_MEDIA_ARTIST, "Ravi")

        assertEquals(listOf(testSong(1).mediaUri), played(spoken("ravi")))
        assertEquals(listOf(testSong(5).mediaUri), played(spoken("play ravi", artist)))
        // The name the assistant understood is what is searched for, not the whole sentence.
        assertEquals("Ravi", found.searched.last())
    }

    @Test
    fun `songs found only by their album or artist text are played when nothing else fits`() {
        found.results = SearchResults(songs = listOf(testSong(1, "Yaman"), testSong(2, "Todi")))

        assertEquals(listOf(testSong(1).mediaUri, testSong(2).mediaUri), played(spoken("evening")))
    }

    @Test
    fun `words that find nothing play nothing, and the driver is told`() {
        val answer = requireNotNull(carPlay.queueFor(spoken("no such thing")))

        assertThrows(ExecutionException::class.java) { answer.get() }
        assertEquals(listOf(NothingToPlay.NOTHING_FOUND), told)
    }

    @Test
    fun `play with no words goes on with what was playing last`() {
        library.songs = listOf(testSong(1))
        saved = MediaItemsWithStartPosition(listOf(playableItem(testSong(4).mediaUri)), 0, 5_000)

        val queue = requireNotNull(carPlay.queueFor(spoken(""))).get()

        assertEquals(listOf(testSong(4).mediaUri), queue.mediaItems.map { it.mediaId })
        assertEquals(5_000, queue.startPositionMs)
    }

    @Test
    fun `play with no words and nothing saved plays the songs of the library`() {
        library.songs = listOf(testSong(1), testSong(2), testSong(3))

        assertEquals(library.songs.map { it.mediaUri }.toSet(), played(spoken(" ?? ")).toSet())
    }

    @Test
    fun `play with no words and an empty library plays nothing, and the driver is told`() {
        val answer = requireNotNull(carPlay.queueFor(spoken("")))

        assertThrows(ExecutionException::class.java) { answer.get() }
        assertEquals(listOf(NothingToPlay.NO_SONGS), told)
    }

    @Test
    fun `an item with an address is not a spoken request`() {
        val sly = spoken("bhairavi").buildUpon().setUri("file:///data/secret").build()

        assertNull(carPlay.queueFor(sly))
        assertNull(carPlay.queueFor(MediaItem.Builder().setMediaId("album.7").build()))
        assertTrue(found.searched.isEmpty())
    }
}
