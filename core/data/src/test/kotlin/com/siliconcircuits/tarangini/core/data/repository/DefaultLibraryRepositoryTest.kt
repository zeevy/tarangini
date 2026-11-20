/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.LetterCount
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultLibraryRepositoryTest {
    private val database = testDatabase()
    private val library = database.libraryDao()
    private var nextFileId = 1

    @After
    fun closeDatabase() = database.close()

    private fun repository() = DefaultLibraryRepository(library)

    /** Saves a song with the next file id, because a local song always has a number as its file id. */
    @Suppress("LongParameterList") // Each test sets only the values it is about.
    private suspend fun song(
        title: String,
        artist: String = "Artist",
        album: String = "Album",
        folderId: Long? = null,
        disc: Int? = null,
        track: Int? = null,
        durationMs: Long = 180_000,
        dateAdded: Long = 0,
    ): Long = database.addSong(title, artist, album, "${nextFileId++}", folderId, disc, track, durationMs, dateAdded)

    private suspend fun threeSongs() {
        song("a", artist = "Zed", album = "Beta", durationMs = 200, dateAdded = 1)
        song("b", artist = "Mia", album = "Gamma", durationMs = 100, dateAdded = 3)
        song("c", artist = "Amy", album = "Alpha", durationMs = 300, dateAdded = 2)
    }

    private suspend fun DefaultLibraryRepository.titles(sort: SongSort) = songs(sort, Page()).map { it.title }

    @Test
    fun `all songs come by title, by artist, by album, newest first and shortest first`() = runTest {
        threeSongs()
        val repository = repository()

        assertEquals(listOf("a", "b", "c"), repository.titles(SongSort.TITLE))
        assertEquals(listOf("c", "b", "a"), repository.titles(SongSort.ARTIST))
        assertEquals(listOf("c", "a", "b"), repository.titles(SongSort.ALBUM))
        assertEquals(listOf("b", "c", "a"), repository.titles(SongSort.DATE_ADDED))
        assertEquals(listOf("b", "a", "c"), repository.titles(SongSort.DURATION))
    }

    @Test
    fun `a list is read one page at a time`() = runTest {
        threeSongs()
        val repository = repository()
        val first = Page(limit = 2)

        assertEquals(listOf("a", "b"), repository.songs(SongSort.TITLE, first).map { it.title })
        assertEquals(listOf("c"), repository.songs(SongSort.TITLE, first.next()).map { it.title })
        assertEquals(listOf("Alpha", "Beta"), repository.albums(first).map { it.title })
        assertEquals(listOf("Zed"), repository.artists(first.next()).map { it.name })
    }

    @Test
    fun `a song comes with its names, its numbers and the address the player takes`() = runTest {
        val id =
            song("Bhairavi", artist = "Ravi", album = "Morning", disc = 1, track = 4, durationMs = 9, dateAdded = 5)
        val found = requireNotNull(repository().song(id))

        assertEquals(
            Song(
                id = id,
                mediaUri = "content://media/external/audio/media/1",
                title = "Bhairavi",
                artistId = found.artistId,
                artist = "Ravi",
                albumId = found.albumId,
                album = "Morning",
                durationMs = 9,
                trackNumber = 4,
                discNumber = 1,
                year = null,
                dateAdded = 5,
            ),
            found,
        )
        assertNull(repository().song(id + 1))
    }

    @Test
    fun `an album and an artist come with the number of their songs`() = runTest {
        song("one", artist = "Ravi", album = "Morning")
        song("two", artist = "Ravi", album = "Morning")
        val repository = repository()
        val one = repository.songs(SongSort.TITLE, Page()).first()

        assertEquals(
            listOf(Album(one.albumId, "Morning", one.artistId, "Ravi", year = null, songCount = 2)),
            repository.albums(Page()),
        )
        assertEquals(listOf(Artist(one.artistId, "Ravi", songCount = 2)), repository.artists(Page()))
    }

    @Test
    fun `the songs of an album come in disc and track order`() = runTest {
        song("d2t1", disc = 2, track = 1)
        song("d1t2", disc = 1, track = 2)
        song("d1t1", disc = 1, track = 1)
        song("other", album = "Other")
        val repository = repository()
        val albumId = repository.songs(SongSort.TITLE, Page()).first { it.title == "d1t1" }.albumId

        assertEquals(listOf("d1t1", "d1t2", "d2t1"), repository.songsOfAlbum(albumId, Page()).map { it.title })
        assertEquals(listOf("d2t1"), repository.songsOfAlbum(albumId, Page(limit = 2, offset = 2)).map { it.title })
    }

    @Test
    fun `the albums and the songs of an artist leave out other artists`() = runTest {
        song("b", artist = "Amy", album = "Second")
        song("a", artist = "Amy", album = "First")
        song("x", artist = "Bob", album = "Third")
        val repository = repository()
        val amy = repository.artists(Page()).first { it.name == "Amy" }.id

        assertEquals(listOf("First", "Second"), repository.albumsOfArtist(amy).map { it.title })
        assertEquals(listOf("a", "b"), repository.songsOfArtist(amy, Page()).map { it.title })
        assertEquals(listOf("b"), repository.songsOfArtist(amy, Page(limit = 1, offset = 1)).map { it.title })
    }

    @Test
    fun `folders form a tree, and a folder gives its own songs in pages`() = runTest {
        val music = library.saveFolder("Music", "Music", parentId = null)
        val ragas = library.saveFolder("Music/Ragas", "Ragas", parentId = music)
        song("in music", folderId = music)
        song("raga 1", folderId = ragas)
        song("raga 2", folderId = ragas)
        val repository = repository()

        assertEquals(listOf(Folder(music, parentId = null, name = "Music", path = "Music")), repository.folders(null))
        assertEquals(listOf("Ragas"), repository.folders(music).map { it.name })
        assertEquals(listOf("in music"), repository.songsInFolder(music, Page()).map { it.title })
        assertEquals(listOf("raga 2"), repository.songsInFolder(ragas, Page(limit = 1, offset = 1)).map { it.title })
    }

    @Test
    fun `a song whose file is gone is in no list, but is still found by its id`() = runTest {
        val gone = song("Gone", artist = "Only", album = "Only")
        library.markMissing(listOf(gone), at = 1)
        val repository = repository()

        assertTrue(repository.songs(SongSort.TITLE, Page()).isEmpty())
        assertTrue(repository.albums(Page()).isEmpty())
        assertTrue(repository.artists(Page()).isEmpty())
        assertEquals("Gone", repository.song(gone)?.title)
    }

    @Test
    fun `the first letters come in the order of the list, and their counts give the place of a letter`() = runTest {
        song("banana", artist = "Zed", album = "Second")
        song("Apple", artist = "amy", album = "First")
        song("  avocado", artist = "Amy", album = "first")
        song("9 to 5", artist = "Zed", album = "Second")
        song("\u0C05\u0C2E\u0C4D\u0C2E", artist = "Zed", album = "")
        // A sign that sorts after the Latin letters, and a first character that takes two UTF-16 units.
        song("~ tilde", artist = "Zed", album = "Second")
        song("\uD83C\uDFB5 tune", artist = "Zed", album = "Second")
        val gone = song("Cherry", artist = "Carl", album = "Third")
        library.markMissing(listOf(gone), at = 1)
        val repository = repository()

        val firstLetters = DefaultLibraryLetters(database.letterDao())

        val letters = firstLetters.ofSongs()

        val counts = listOf("9" to 1, "a" to 2, "b" to 1, "~" to 1, "\u0C05" to 1, "\uD83C\uDFB5" to 1)
        val expected = counts.map { (letter, count) ->
            LetterCount(letter, count)
        }
        assertEquals(expected, letters)
        // The songs of "b" start after the songs of the letters before it.
        val before = letters.takeWhile { it.letter != "b" }.sumOf { it.count }
        assertEquals(
            listOf("banana"),
            repository.songs(SongSort.TITLE, Page(limit = 1, offset = before)).map {
                it.title
            },
        )
        // The counts before the last letter give its place too, so every group is where the list has it.
        val last = repository.songs(SongSort.TITLE, Page(limit = 1, offset = letters.dropLast(1).sumOf { it.count }))
        assertEquals(listOf("\uD83C\uDFB5 tune"), last.map { it.title })
        // An album or an artist with no song that can be played is not counted. An empty name has an empty letter.
        assertEquals(listOf(LetterCount("", 1), LetterCount("f", 1), LetterCount("s", 1)), firstLetters.ofAlbums())
        assertEquals(listOf(LetterCount("a", 1), LetterCount("z", 1)), firstLetters.ofArtists())
    }

    @Test
    fun `a change of the library is told to a reader`() = runBlocking {
        val repository = repository()
        val told = Channel<Unit>(Channel.UNLIMITED)
        // Room tells from its own threads, so this test waits in real time.
        val reader = launch(Dispatchers.Default) { repository.changes().collect { told.send(it) } }
        withTimeout(WAIT_MS) { told.receive() }

        val id = song("New")
        withTimeout(WAIT_MS) { told.receive() }

        // A change that leaves the number of songs as it was is told too.
        library.markMissing(listOf(id), at = 1)
        song("Other")
        withTimeout(WAIT_MS) { told.receive() }
        reader.cancel()
    }

    private companion object {
        const val WAIT_MS = 10_000L
    }
}
