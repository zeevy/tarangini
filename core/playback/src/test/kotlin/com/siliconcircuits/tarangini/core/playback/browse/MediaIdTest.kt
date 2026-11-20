/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaIdTest {
    private val everyKind = listOf(
        MediaId.Root,
        MediaId.Library,
        MediaId.Folders,
        MediaId.Favourites,
        MediaId.Recent,
        MediaId.Section(LibraryList.SONGS),
        MediaId.Section(LibraryList.ALBUMS),
        MediaId.Section(LibraryList.ARTISTS),
        MediaId.Letter(LibraryList.SONGS, MediaId.BEFORE_LETTERS),
        MediaId.Letter(LibraryList.ALBUMS, 'a'.code),
        MediaId.Letter(LibraryList.ARTISTS, Character.MAX_CODE_POINT),
        MediaId.Album(12),
        MediaId.Artist(3),
        MediaId.Folder(Long.MAX_VALUE / 10),
        MediaId.Playlists,
        MediaId.Playlist(4),
        MediaId.SongIn(345, MediaId.Playlist(4)),
        MediaId.SongIn(345, MediaId.Album(12)),
        MediaId.SongIn(345, MediaId.Artist(3)),
        MediaId.SongIn(345, MediaId.Folder(9)),
        MediaId.SongIn(345, MediaId.Section(LibraryList.SONGS)),
        MediaId.SongIn(345, MediaId.Letter(LibraryList.SONGS, 0x0C05)),
    )

    @Test
    fun `every id reads back as what it was made from`() {
        everyKind.forEach { id -> assertEquals(id, MediaId.parse(id.encode())) }
    }

    @Test
    fun `no two ids have the same text`() {
        assertEquals(everyKind.size, everyKind.map { it.encode() }.toSet().size)
    }

    @Test
    fun `the text of an id is short and plain`() {
        assertEquals("root", MediaId.Root.encode())
        assertEquals("album.12", MediaId.Album(12).encode())
        assertEquals("songs.97", MediaId.Letter(LibraryList.SONGS, 'a'.code).encode())
        assertEquals("song.345@album.12", MediaId.SongIn(345, MediaId.Album(12)).encode())
    }

    @Test
    fun `a text this app did not make is not an id`() {
        listOf(
            "",
            " ",
            "Root",
            "root ",
            "radio",
            "album",
            "album.",
            "album.x",
            "album.-1",
            "album.+1",
            "album.1.2",
            "album.01",
            "album. 1",
            "album.\u0661",
            "album.1234567890123456789",
            "album.99999999999999999999999999",
            "root.1",
            "folders.1",
            "playlists.1",
            "playlist",
            "playlist.01",
            "song.3@playlists",
            "songs.1114112",
            "song.1",
            "song.1@",
            "song.1@root",
            "song.1@library",
            "song.1@albums",
            "song.1@artists.97",
            "song.1@song.2@album.3",
            "song.x@album.1",
            "album.1@album.2",
            "@album.1",
            "content://media/external/audio/media/1",
            "../../etc/passwd",
            "album.1\u0000",
            "a".repeat(100_000),
            "@".repeat(100_000),
            "song.1@album.1" + "@".repeat(100),
            "song.1@album." + "1".repeat(60),
        ).forEach { text -> assertNull(text.take(40), MediaId.parse(text)) }
    }
}
