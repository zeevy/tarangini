/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.dao.SongRow
import com.siliconcircuits.tarangini.core.data.mapper.toAlbum
import com.siliconcircuits.tarangini.core.data.mapper.toArtist
import com.siliconcircuits.tarangini.core.data.mapper.toFolder
import com.siliconcircuits.tarangini.core.data.mapper.toSong
import com.siliconcircuits.tarangini.core.data.mediastore.StorageNames
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The library from the database of the app. Every list is one query with a limit; nothing is kept in memory. */
internal class DefaultLibraryRepository @Inject constructor(
    private val library: LibraryDao,
    private val storageNames: StorageNames,
) : LibraryRepository {
    override fun changes(): Flow<Unit> = library.hasSongs().map { }

    override suspend fun songs(sort: SongSort, page: Page): List<Song> = when (sort) {
        SongSort.TITLE -> library.songsByTitle(page.limit, page.offset)
        SongSort.ARTIST -> library.songsByArtist(page.limit, page.offset)
        SongSort.ALBUM -> library.songsByAlbum(page.limit, page.offset)
        SongSort.DATE_ADDED -> library.songsByDateAdded(page.limit, page.offset)
        SongSort.DURATION -> library.songsByDuration(page.limit, page.offset)
    }.map(SongRow::toSong)

    override suspend fun song(id: Long): Song? = library.song(id)?.toSong()

    override suspend fun albums(page: Page): List<Album> = library.albums(page.limit, page.offset).map { it.toAlbum() }

    override suspend fun artists(page: Page): List<Artist> =
        library.artists(page.limit, page.offset).map { it.toArtist() }

    override suspend fun songsOfAlbum(albumId: Long, page: Page): List<Song> =
        library.songsOfAlbum(albumId, page.limit, page.offset).map(SongRow::toSong)

    override suspend fun albumsOfArtist(artistId: Long): List<Album> =
        library.albumsOfArtist(artistId).map { it.toAlbum() }

    override suspend fun songsOfArtist(artistId: Long, page: Page): List<Song> =
        library.songsOfArtist(artistId, page.limit, page.offset).map(SongRow::toSong)

    override suspend fun folders(parentId: Long?): List<Folder> {
        val folders = library.childFolders(parentId).map { it.toFolder() }
        // A top folder is a storage. Its saved name is the code MediaStore has for it.
        return if (parentId == null) folders.map { it.copy(name = storageNames.nameOf(it.name)) } else folders
    }

    override suspend fun songsInFolder(folderId: Long, page: Page): List<Song> =
        library.songsInFolder(folderId, page.limit, page.offset).map(SongRow::toSong)
}
