/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import kotlinx.coroutines.flow.Flow

/**
 * The songs, albums, artists and folders of the library. The phone screens
 * and the playback service (for the car) read the library only through this.
 *
 * A song whose file is gone is in no list. A list that can be long is read
 * in pages; the albums of an artist and the folders in a folder are short
 * and come whole.
 *
 * Each page is its own read. While a scan writes, rows move between two
 * reads, so a row can come twice or be left out. A reader that keeps
 * several pages reads them again from the first page after [changes], and
 * never shows the same id twice.
 */
interface LibraryRepository {
    /**
     * Gives a value when the reading starts and again each time the library
     * changed. A list reads its pages again then. A scan of many files gives
     * many values in a short time, so a reader waits a moment for the last
     * one before it reads.
     */
    fun changes(): Flow<Unit>

    /** One page of all songs in the order [sort]. */
    suspend fun songs(sort: SongSort, page: Page): List<Song>

    /** The song with this id, also when its file is gone, or null when there is none. */
    suspend fun song(id: Long): Song?

    /** One page of all albums, by title. */
    suspend fun albums(page: Page): List<Album>

    /** One page of all artists, by name. */
    suspend fun artists(page: Page): List<Artist>

    /**
     * One page of the songs of one album in album order: by disc, then by
     * track. It is paged because the songs with no album tag share one album.
     */
    suspend fun songsOfAlbum(albumId: Long, page: Page): List<Song>

    /** The albums of one artist, by title. */
    suspend fun albumsOfArtist(artistId: Long): List<Album>

    /** One page of the songs of one artist, by title. */
    suspend fun songsOfArtist(artistId: Long, page: Page): List<Song>

    /** The folders directly inside [parentId], or the top folders when it is null, by name. */
    suspend fun folders(parentId: Long?): List<Folder>

    /** One page of the songs directly inside a folder, by file name. */
    suspend fun songsInFolder(folderId: Long, page: Page): List<Song>
}
