/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local

import android.database.SQLException
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.entity.FavouriteSongEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryDaoTest {
    private val database = testDatabase()
    private val library = database.libraryDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `saving the same artist, album or folder again returns the same id`() = runTest {
        val artist = library.saveArtist("Asha", "asha")
        val album = library.saveAlbum("Hits", "hits", artist, year = 1990)
        val folder = library.saveFolder("Music/Old", "Old", parentId = null)
        library.saveSong(songEntity("Song", "1", artist, album))

        assertEquals(artist, library.saveArtist("Asha", "asha"))
        assertEquals(album, library.saveAlbum("Hits", "hits", artist, year = 1991))
        assertEquals(folder, library.saveFolder("Music/Old", "Old", parentId = null))
        assertEquals(1991, library.albums(limit = 10, offset = 0).single().album.year)
        assertEquals(album, library.saveAlbum("Hits", "hits", artist, year = null))
        assertEquals(1991, library.albums(limit = 10, offset = 0).single().album.year)
    }

    @Test
    fun `two spellings of one name are one artist and one album, with the first spelling`() = runTest {
        database.addSong("One", artist = "ABBA", album = "Gold")
        database.addSong("Two", artist = " Abba", album = "GOLD")

        assertEquals(listOf("ABBA" to 2), library.artists(10, 0).map { it.artist.name to it.songCount })
        assertEquals(listOf("Gold" to 2), library.albums(10, 0).map { it.album.title to it.songCount })
    }

    @Test
    fun `songs by artist keep each artist together, and songs by album keep each album together`() = runTest {
        database.addSong("b", artist = "Amy", album = "Same")
        database.addSong("a", artist = "Zed", album = "Same")
        database.addSong("c", artist = "Amy", album = "Same")

        assertEquals(listOf("b", "c", "a"), library.songsByArtist(10, 0).map { it.song.title })
        assertEquals(listOf("b", "c", "a"), library.songsByAlbum(10, 0).map { it.song.title })
    }

    @Test
    fun `a list of ids longer than one statement takes is marked and removed in full`() = runTest {
        val artist = library.saveArtist("A", "a")
        val album = library.saveAlbum("B", "b", artist, year = null)
        val ids = List(2 * LibraryDao.MAX_IDS + 1) { library.saveSong(songEntity("s$it", "$it", artist, album)) }

        library.markMissing(ids, at = 1)
        assertTrue(library.songsByTitle(10, 0).isEmpty())
        assertEquals(ids.size, library.songKeys(SongSource.LOCAL).count { it.missingSince == 1L })

        library.deleteSongs(ids)
        assertTrue(library.songKeys(SongSource.LOCAL).isEmpty())
    }

    @Test
    fun `two artists can each have an album with the same title`() = runTest {
        database.addSong("One", artist = "A", album = "Best of")
        database.addSong("Two", artist = "B", album = "Best of")

        assertEquals(listOf("A", "B"), library.albums(10, 0).map { it.artistName })
    }

    @Test
    fun `saving a song again updates it and keeps its id`() = runTest {
        val id = database.addSong("Old title", sourceId = "42")

        val again = database.addSong("New title", sourceId = "42")

        assertEquals(id, again)
        assertEquals(listOf("New title"), library.songsByTitle(10, 0).map { it.song.title })
    }

    @Test
    fun `saving a song again keeps its favourite, playlist place, play count and queue place`() = runTest {
        val id = database.addSong("Song", sourceId = "42")
        database.favouriteDao().addSong(FavouriteSongEntity(id, addedAt = 1))
        val playlist = database.addPlaylist("Mine")
        database.playlistDao().addItem(playlist, id, at = 1)
        database.historyDao().recordSongPlay(id, at = 1)
        database.queueDao().replaceQueue(listOf(id))

        database.addSong("Song, tagged again", sourceId = "42")

        assertEquals(listOf(id), database.favouriteDao().favouriteSongs(10, 0).map { it.song.id })
        assertEquals(listOf(id), database.playlistDao().items(playlist, 10, 0).map { it.row.song.id })
        assertEquals(listOf(id), database.historyDao().recentSongs(10).map { it.song.id })
        assertEquals(listOf(id), database.queueDao().queue().map { it.row.song.id })
    }

    @Test
    fun `a song needs an artist and an album that exist`() {
        val song = songEntity("Song", "1", artistId = 99, albumId = 99)

        assertThrows(SQLException::class.java) { runBlocking { library.saveSong(song) } }
    }

    @Test
    fun `songs sort by title without regard to case, and in pages`() = runTest {
        listOf("banana", "Apple", "cherry").forEach { database.addSong(it) }

        assertEquals(listOf("Apple", "banana"), library.songsByTitle(limit = 2, offset = 0).map { it.song.title })
        assertEquals(listOf("cherry"), library.songsByTitle(limit = 2, offset = 2).map { it.song.title })
    }

    @Test
    fun `songs sort by artist, by album, by date added and by duration`() = runTest {
        database.addSong("s1", artist = "Zed", album = "B", durationMs = 300, dateAdded = 1)
        database.addSong("s2", artist = "Amy", album = "C", durationMs = 100, dateAdded = 3)
        database.addSong("s3", artist = "Moe", album = "A", durationMs = 200, dateAdded = 2)

        assertEquals(listOf("s2", "s3", "s1"), library.songsByArtist(10, 0).map { it.song.title })
        assertEquals(listOf("s3", "s1", "s2"), library.songsByAlbum(10, 0).map { it.song.title })
        assertEquals(listOf("s2", "s3", "s1"), library.songsByDateAdded(10, 0).map { it.song.title })
        assertEquals(listOf("s2", "s3", "s1"), library.songsByDuration(10, 0).map { it.song.title })
    }

    @Test
    fun `the songs of an album come in disc and track order`() = runTest {
        database.addSong("d2t1", disc = 2, track = 1)
        database.addSong("d1t2", disc = 1, track = 2)
        database.addSong("d1t1", disc = 1, track = 1)
        database.addSong("other", album = "Other")
        val albumId = library.songsByTitle(10, 0).first { it.song.title == "d1t1" }.song.albumId

        assertEquals(listOf("d1t1", "d1t2", "d2t1"), library.songsOfAlbum(albumId).map { it.song.title })
    }

    @Test
    fun `the songs and the albums of an artist leave out other artists`() = runTest {
        database.addSong("b", artist = "Amy", album = "Second")
        database.addSong("a", artist = "Amy", album = "First")
        database.addSong("x", artist = "Zed", album = "Third")
        val amy = library.artists(10, 0).first { it.artist.name == "Amy" }

        assertEquals(2, amy.songCount)
        assertEquals(listOf("a", "b"), library.songsOfArtist(amy.artist.id, 10, 0).map { it.song.title })
        assertEquals(listOf("First", "Second"), library.albumsOfArtist(amy.artist.id).map { it.album.title })
    }

    @Test
    fun `folders form a tree and hold their songs by file name`() = runTest {
        val music = library.saveFolder("Music", "Music", parentId = null)
        val old = library.saveFolder("Music/Old", "Old", parentId = music)
        library.saveFolder("Music/New", "New", parentId = music)
        database.addSong("b", folderId = old)
        database.addSong("a", folderId = old)

        assertEquals(listOf("Music"), library.childFolders(null).map { it.name })
        assertEquals(listOf("New", "Old"), library.childFolders(music).map { it.name })
        assertEquals(listOf("a.mp3", "b.mp3"), library.songsInFolder(old).map { it.song.fileName })
    }

    @Test
    fun `a song whose file is gone leaves the lists, keeps its favourite and comes back`() = runTest {
        val id = database.addSong("Song", sourceId = "42")
        database.addSong("Other", album = "Other album")
        database.favouriteDao().addSong(FavouriteSongEntity(id, addedAt = 1))

        library.markMissing(listOf(id), at = 5)
        library.markMissing(listOf(id), at = 9)

        assertEquals(listOf("Other"), library.songsByTitle(10, 0).map { it.song.title })
        assertEquals(listOf("Other album"), library.albums(10, 0).map { it.album.title })
        assertEquals(1, library.artists(10, 0).single().songCount)
        assertTrue(database.favouriteDao().favouriteSongs(10, 0).isEmpty())
        assertEquals(5L, library.songKeys(SongSource.LOCAL).first { it.id == id }.missingSince)
        assertEquals(5L, library.song(id)?.song?.missingSince)

        database.addSong("Song", sourceId = "42")

        assertEquals(listOf(id), database.favouriteDao().favouriteSongs(10, 0).map { it.song.id })
        assertNull(library.song(id)?.song?.missingSince)
    }

    @Test
    fun `removing a song removes its favourite, playlist place, play count, queue place and search row`() = runTest {
        val id = database.addSong("Gone")
        val kept = database.addSong("Kept")
        database.favouriteDao().addSong(FavouriteSongEntity(id, addedAt = 1))
        val playlist = database.addPlaylist("Mine")
        database.playlistDao().addItem(playlist, id, at = 1)
        database.playlistDao().addItem(playlist, kept, at = 2)
        database.historyDao().recordSongPlay(id, at = 1)
        database.queueDao().replaceQueue(listOf(id, kept))

        library.deleteSongs(listOf(id))

        assertTrue(database.favouriteDao().favouriteSongs(10, 0).isEmpty())
        assertEquals(listOf(kept), database.playlistDao().items(playlist, 10, 0).map { it.row.song.id })
        assertTrue(database.historyDao().recentSongs(10).isEmpty())
        assertEquals(listOf(kept), database.queueDao().queue().map { it.row.song.id })
        assertTrue(library.searchSongs(requireNotNull(FtsQuery.prefix("gone")), 10).isEmpty())
        assertNull(library.song(id))
    }

    @Test
    fun `albums, artists and folders with nothing left are removed`() = runTest {
        val top = library.saveFolder("A", "A", parentId = null)
        val inner = library.saveFolder("A/B", "B", parentId = top)
        val id = database.addSong("Song", artist = "Solo", album = "Single", folderId = inner)
        database.addSong("Stays", artist = "Other", album = "Other album")

        library.deleteSongs(listOf(id))

        assertEquals(1, library.deleteUnusedAlbums())
        assertEquals(1, library.deleteUnusedArtists())
        assertEquals(1, library.deleteUnusedFolders())
        assertEquals(1, library.deleteUnusedFolders())
        assertEquals(0, library.deleteUnusedFolders())
        assertEquals(listOf("Other"), library.artists(10, 0).map { it.artist.name })
    }

    @Test
    fun `search finds a song by the start of a word in its title, artist or album`() = runTest {
        database.addSong("Morning Raga", artist = "Ravi Shankar", album = "Live in Delhi")
        database.addSong("Evening Song", artist = "Someone", album = "Nights")

        assertEquals(listOf("Morning Raga"), search("morn"))
        assertEquals(listOf("Morning Raga"), search("shank"))
        assertEquals(listOf("Morning Raga"), search("delhi"))
        assertEquals(listOf("Morning Raga"), search("raga ravi"))
        assertTrue(search("zzz").isEmpty())
    }

    @Test
    fun `search ignores signs between words`() = runTest {
        database.addSong("Rock - Roll", artist = "AC/DC")

        assertEquals(listOf("Rock - Roll"), search("rock - roll"))
        assertEquals(listOf("Rock - Roll"), search("rock & roll"))
        assertEquals(listOf("Rock - Roll"), search("ac/dc"))
        assertEquals(listOf("Rock - Roll"), search("AC - DC"))
    }

    @Test
    fun `search works for Telugu text and follows a changed tag`() = runTest {
        database.addSong("ఎందరో మహానుభావులు", sourceId = "1")
        assertEquals(1, search("ఎంద").size)

        database.addSong("Endaro", sourceId = "1")

        assertTrue(search("ఎంద").isEmpty())
        assertEquals(listOf("Endaro"), search("endaro"))
    }

    @Test
    fun `search text with operators in it is only text`() = runTest {
        database.addSong("Plain")
        val hostile = listOf("\"plain", "plain OR zzz*", "* NEAR -plain", "title:plain)", "plain'; -- x", "(((")

        hostile.forEach { text -> search(text) }

        assertTrue(search("plain OR zzz*").isEmpty())
        assertEquals(listOf("Plain"), library.songsByTitle(10, 0).map { it.song.title })
    }

    private suspend fun search(text: String): List<String> =
        FtsQuery.prefix(text)?.let { query -> library.searchSongs(query, 10).map { it.song.title } }.orEmpty()
}
