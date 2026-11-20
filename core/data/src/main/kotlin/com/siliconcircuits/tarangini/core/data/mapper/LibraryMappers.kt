/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mapper

import com.siliconcircuits.tarangini.core.data.local.dao.AlbumRow
import com.siliconcircuits.tarangini.core.data.local.dao.ArtistRow
import com.siliconcircuits.tarangini.core.data.local.dao.SongRow
import com.siliconcircuits.tarangini.core.data.local.entity.FolderEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource
import com.siliconcircuits.tarangini.core.data.mediastore.localMediaUri
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Song

/** The song as the rest of the app sees it. */
internal fun SongRow.toSong() = Song(
    id = song.id,
    // A new kind of storage must say here what its songs are played from, or this does not compile.
    mediaUri = when (song.source) {
        SongSource.LOCAL -> localMediaUri(song.sourceId)
    },
    title = song.title,
    artistId = song.artistId,
    artist = artistName,
    albumId = song.albumId,
    album = albumTitle,
    durationMs = song.durationMs,
    trackNumber = song.trackNumber,
    discNumber = song.discNumber,
    year = song.year,
    dateAdded = song.dateAdded,
)

internal fun AlbumRow.toAlbum() = Album(
    id = album.id,
    title = album.title,
    artistId = album.artistId,
    artist = artistName,
    year = album.year,
    songCount = songCount,
)

internal fun ArtistRow.toArtist() = Artist(id = artist.id, name = artist.name, songCount = songCount)

internal fun FolderEntity.toFolder() = Folder(id = id, parentId = parentId, name = name, path = path)
