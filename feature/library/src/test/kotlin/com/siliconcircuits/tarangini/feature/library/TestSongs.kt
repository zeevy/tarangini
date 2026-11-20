/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Song

/** A song for tests. Its address is not one of the phone, so no cover picture is asked for. */
internal fun testSong(
    id: Long,
    title: String = "Song $id",
    artist: String = "Artist",
    album: String = "Album",
    artistId: Long = 1,
    albumId: Long = 1,
    durationMs: Long = 185_000,
) = Song(
    id = id,
    mediaUri = "test:$id",
    title = title,
    artistId = artistId,
    artist = artist,
    albumId = albumId,
    album = album,
    durationMs = durationMs,
    trackNumber = null,
    discNumber = null,
    year = null,
    dateAdded = id,
)

internal fun testAlbum(id: Long, title: String, artist: String = "Artist", artistId: Long = 1, songCount: Int = 10) =
    Album(id = id, title = title, artistId = artistId, artist = artist, year = null, songCount = songCount)

internal fun testArtist(id: Long, name: String, songCount: Int = 12) =
    Artist(id = id, name = name, songCount = songCount)

internal fun testFolder(id: Long, name: String, parentId: Long? = null) =
    Folder(id = id, parentId = parentId, name = name, path = name)
