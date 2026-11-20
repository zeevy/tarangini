/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A library for tests, kept in lists a test fills. The lists come back in
 * the order the real library gives for plain names. It does not copy the
 * finer rules of the database: two equal names keep the order of the list,
 * and the songs of a folder come as the test put them. [folderOfSong] says
 * in which folder a song is; a song that is not in it is in no folder.
 */
class FakeLibraryRepository : LibraryRepository {
    var songs: List<Song> = emptyList()
    var albums: List<Album> = emptyList()
    var artists: List<Artist> = emptyList()
    var folders: List<Folder> = emptyList()
    var folderOfSong: Map<Long, Long> = emptyMap()

    /** A test raises this number to tell the readers that the library changed, the way a scan does. */
    val version = MutableStateFlow(0)

    override fun changes(): Flow<Unit> = version.map { }

    override suspend fun songs(sort: SongSort, page: Page): List<Song> = when (sort) {
        SongSort.TITLE -> songs.sortedBy { it.title.lowercase() }
        SongSort.ARTIST -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        SongSort.ALBUM -> songs.sortedWith(compareBy<Song> { it.album.lowercase() }.then(albumOrder))
        SongSort.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        SongSort.DURATION -> songs.sortedBy { it.durationMs }
    }.page(page)

    override suspend fun song(id: Long): Song? = songs.firstOrNull { it.id == id }

    override suspend fun albums(page: Page): List<Album> = albums.sortedBy { it.title.lowercase() }.page(page)

    override suspend fun artists(page: Page): List<Artist> = artists.sortedBy { it.name.lowercase() }.page(page)

    override suspend fun songsOfAlbum(albumId: Long, page: Page): List<Song> =
        songs.filter { it.albumId == albumId }.sortedWith(albumOrder).page(page)

    override suspend fun albumsOfArtist(artistId: Long): List<Album> =
        albums.filter { it.artistId == artistId }.sortedBy { it.title.lowercase() }

    override suspend fun songsOfArtist(artistId: Long, page: Page): List<Song> =
        songs.filter { it.artistId == artistId }.sortedBy { it.title.lowercase() }.page(page)

    override suspend fun folders(parentId: Long?): List<Folder> =
        folders.filter { it.parentId == parentId }.sortedBy { it.name }

    override suspend fun songsInFolder(folderId: Long, page: Page): List<Song> =
        songs.filter { folderOfSong[it.id] == folderId }.page(page)

    private companion object {
        val albumOrder = compareBy<Song>({ it.discNumber ?: 0 }, { it.trackNumber ?: 0 }, { it.title.lowercase() })
    }
}

private fun <T> List<T>.page(page: Page): List<T> = drop(page.offset).take(page.limit)
