/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import androidx.media3.common.MediaItem
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryLetters
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.testAlbum
import com.siliconcircuits.tarangini.core.testing.library.testArtist
import com.siliconcircuits.tarangini.core.testing.library.testFolder
import com.siliconcircuits.tarangini.core.testing.library.testSong
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowseTreeTest {
    private val library = FakeLibraryRepository()
    private val labels =
        BrowseLabels("Library", "Songs", "Albums", "Artists", "Folders", "Unknown album", "Unknown artist", null)
    private val tree = BrowseTree(library, FakeLibraryLetters(library), BrowseItems(labels))

    private suspend fun under(id: MediaId, tabLimit: Int = MAX_TABS): List<MediaItem> =
        requireNotNull(tree.children(id, tabLimit))

    private fun List<MediaItem>.titles() = map { it.mediaMetadata.title.toString() }

    private fun List<MediaItem>.ids() = map { it.id() }

    private fun MediaItem.id(): MediaId = requireNotNull(MediaId.parse(mediaId))

    /** A library too long for one list: 60 songs under "a", 50 under "b", 3 that start with a digit or a sign. */
    private fun longLibrary() {
        val a = (1L..60L).map { testSong(it, title = "a song %03d".format(it)) }
        val b = (61L..110L).map { testSong(it, title = "B song %03d".format(it)) }
        val other =
            listOf(testSong(111, title = "9 to 5"), testSong(112, title = "(live)"), testSong(113, title = "1999"))
        library.songs = a + b + other
    }

    @Test
    fun `the top shows the Library tab, and the Library shows its four lists`() = runTest {
        assertEquals(listOf("Library"), under(MediaId.Root).titles())
        assertEquals(listOf("Songs", "Albums", "Artists", "Folders"), under(MediaId.Library).titles())
    }

    @Test
    fun `the top never shows more tabs than the car allows, and never more than four`() = runTest {
        (0..MAX_TABS).forEach { limit -> assertTrue(under(MediaId.Root, limit).size <= limit) }
        assertTrue(under(MediaId.Root, Int.MAX_VALUE).size <= MAX_TABS)
    }

    @Test
    fun `a short list of songs comes whole, by title, and each song names the list it is in`() = runTest {
        library.songs = listOf(testSong(1, title = "Beta"), testSong(2, title = "alpha"))

        val songs = under(MediaId.Section(LibraryList.SONGS))

        assertEquals(listOf("alpha", "Beta"), songs.titles())
        val list = MediaId.Section(LibraryList.SONGS)
        assertEquals(listOf(MediaId.SongIn(2, list), MediaId.SongIn(1, list)), songs.ids())
        assertNull(songs.first().localConfiguration)
    }

    @Test
    fun `a long list of songs comes as its letters, with digits and signs under one node`() = runTest {
        longLibrary()

        val letters = under(MediaId.Section(LibraryList.SONGS))

        assertEquals(listOf("#", "A", "B"), letters.titles())
        assertTrue(letters.all { it.mediaMetadata.isBrowsable == true && it.mediaMetadata.isPlayable == false })
        val underB = under(letters[2].id())
        assertEquals(50, underB.size)
        assertEquals("B song 061", underB.titles().first())
        assertEquals(listOf("(live)", "1999", "9 to 5"), under(letters[0].id()).titles())
    }

    @Test
    fun `no node gives more rows than one read of the library`() = runTest {
        library.songs = (1L..700L).map { testSong(it, title = "a %04d".format(it)) }

        val letter = under(MediaId.Section(LibraryList.SONGS)).ids().single()

        assertEquals(MAX_ROWS, under(letter).size)
        assertEquals(MAX_ROWS, under(MediaId.Album(1)).size)
    }

    @Test
    fun `a letter the library does not have gives an empty list`() = runTest {
        longLibrary()

        assertTrue(under(MediaId.Letter(LibraryList.SONGS, 'z'.code)).isEmpty())
        assertTrue(under(MediaId.Letter(LibraryList.ALBUMS, 'z'.code)).isEmpty())
    }

    @Test
    fun `albums and artists open their songs, and a name the files do not give gets a text`() = runTest {
        library.albums = listOf(testAlbum(1, "Kind of Blue", artist = "Miles"), testAlbum(2, "", artist = ""))
        library.artists = listOf(testArtist(1, "Miles"), testArtist(2, ""))
        library.songs = listOf(testSong(1, albumId = 1, artistId = 1), testSong(2, albumId = 2, artistId = 2))

        val albums = under(MediaId.Section(LibraryList.ALBUMS))
        val artists = under(MediaId.Section(LibraryList.ARTISTS))

        assertEquals(listOf("Unknown album", "Kind of Blue"), albums.titles())
        assertEquals("Miles", albums[1].mediaMetadata.artist)
        assertEquals(listOf("Unknown artist", "Miles"), artists.titles())
        assertEquals(listOf(MediaId.SongIn(1, MediaId.Album(1))), under(MediaId.Album(1)).ids())
        assertEquals(listOf(MediaId.SongIn(2, MediaId.Artist(2))), under(MediaId.Artist(2)).ids())
    }

    @Test
    fun `a long list of albums comes as its letters`() = runTest {
        library.albums =
            (1L..150L).map { testAlbum(it, if (it <= 100) "Album %03d".format(it) else "Zebra %03d".format(it)) }

        val letters = under(MediaId.Section(LibraryList.ALBUMS))

        assertEquals(listOf("A", "Z"), letters.titles())
        val underZ = under(letters[1].id())
        assertEquals(50, underZ.size)
        assertEquals(MediaId.Album(101), underZ.ids().first())
    }

    @Test
    fun `a folder shows the folders in it first, then its songs`() = runTest {
        library.folders = listOf(testFolder(1, "Music"), testFolder(2, "Old", parentId = 1))
        library.songs = listOf(testSong(1), testSong(2))
        library.folderOfSong = mapOf(1L to 1L, 2L to 2L)

        assertEquals(listOf(MediaId.Folder(1)), under(MediaId.Folders).ids())
        assertEquals(listOf(MediaId.Folder(2), MediaId.SongIn(1, MediaId.Folder(1))), under(MediaId.Folder(1)).ids())
    }

    @Test
    fun `every item either opens a list or plays, never both`() = runTest {
        longLibrary()
        library.albums = listOf(testAlbum(1, "Album"))
        library.artists = listOf(testArtist(1, "Artist"))
        library.folders = listOf(testFolder(1, "Music"))
        library.folderOfSong = mapOf(1L to 1L)

        val seen = walk(MediaId.Root, depth = 0)

        assertTrue(seen.isNotEmpty())
        seen.forEach { (item, _) ->
            assertNotEquals(item.mediaId, item.mediaMetadata.isBrowsable, item.mediaMetadata.isPlayable)
        }
    }

    @Test
    fun `a song is never more than four taps below a tab`() = runTest {
        longLibrary()
        library.albums = listOf(testAlbum(1, "Album"))
        library.artists = listOf(testArtist(1, "Artist"))

        val deepest = walk(MediaId.Root, depth = 0).maxOf { (_, depth) -> depth }

        // Tab, list, letter, song. Folders go as deep as the folders of the phone, so the test has none nested.
        assertEquals(MAX_DEPTH, deepest)
    }

    @Test
    fun `a tap on a song plays the list it was shown in, from that song on`() = runTest {
        library.songs = listOf(testSong(1, albumId = 7), testSong(2, albumId = 7), testSong(3, albumId = 8))

        val queue = requireNotNull(tree.queue(MediaId.SongIn(2, MediaId.Album(7))))

        assertEquals(listOf(1L, 2L), queue.songs.map { it.id })
        assertEquals(1, queue.index)
    }

    @Test
    fun `a tap on a song under a letter plays the songs of that letter`() = runTest {
        longLibrary()
        val letterB = MediaId.Letter(LibraryList.SONGS, 'b'.code)

        val queue = requireNotNull(tree.queue(MediaId.SongIn(70, letterB)))

        assertEquals(50, queue.songs.size)
        assertEquals(70L, queue.songs[queue.index].id)
    }

    @Test
    fun `a song that left its list plays alone, and a song the library does not have plays nothing`() = runTest {
        library.songs = listOf(testSong(1, albumId = 7))

        val moved = requireNotNull(tree.queue(MediaId.SongIn(1, MediaId.Album(99))))

        assertEquals(listOf(1L), moved.songs.map { it.id })
        assertEquals(0, moved.index)
        assertNull(tree.queue(MediaId.SongIn(404, MediaId.Album(7))))
    }

    @Test
    fun `an item is found by its id, and a song has nothing under it`() = runTest {
        library.songs = listOf(testSong(1))
        val song = MediaId.SongIn(1, MediaId.Album(1))

        assertEquals("Library", tree.item(MediaId.Library)?.mediaMetadata?.title)
        assertEquals("Songs", tree.item(MediaId.Section(LibraryList.SONGS))?.mediaMetadata?.title)
        assertEquals(true, tree.item(MediaId.Album(5))?.mediaMetadata?.isBrowsable)
        assertEquals(song.encode(), tree.item(song)?.mediaId)
        assertEquals(true, tree.item(song)?.mediaMetadata?.isPlayable)
        assertNull(tree.item(MediaId.SongIn(404, MediaId.Album(1))))
        assertNull(tree.children(song, MAX_TABS))
        assertFalse(tree.root.mediaMetadata.isPlayable == true)
    }

    /** Every item below [parent] with how many taps below the top it is. */
    private suspend fun walk(parent: MediaId, depth: Int): List<Pair<MediaItem, Int>> = under(parent).flatMap { item ->
        val id = requireNotNull(MediaId.parse(item.mediaId))
        listOf(item to depth) + if (id is MediaId.SongIn) emptyList() else walk(id, depth + 1)
    }

    private companion object {
        const val MAX_TABS = 4
        const val MAX_ROWS = 500
        const val MAX_DEPTH = 3
    }
}
