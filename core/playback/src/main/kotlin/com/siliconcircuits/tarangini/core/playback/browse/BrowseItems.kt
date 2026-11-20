/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.playback.R
import com.siliconcircuits.tarangini.core.playback.artwork.ArtworkAddress
import com.siliconcircuits.tarangini.core.playback.mapper.toMediaItem

/**
 * The texts of the nodes that are not in the library, in the language of the phone.
 *
 * @property libraryIcon the white icon of the Library tab, as an address the car can read
 */
internal data class BrowseLabels(
    val library: String,
    val songs: String,
    val albums: String,
    val artists: String,
    val folders: String,
    val unknownAlbum: String,
    val unknownArtist: String,
    val libraryIcon: Uri?,
)

/** The labels from the string resources of the app. */
internal fun browseLabels(context: Context) = BrowseLabels(
    library = context.getString(R.string.car_library),
    songs = context.getString(R.string.car_songs),
    albums = context.getString(R.string.car_albums),
    artists = context.getString(R.string.car_artists),
    folders = context.getString(R.string.car_folders),
    unknownAlbum = context.getString(R.string.car_unknown_album),
    unknownArtist = context.getString(R.string.car_unknown_artist),
    libraryIcon = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.packageName)
        .appendPath(context.resources.getResourceTypeName(R.drawable.ic_car_library))
        .appendPath(context.resources.getResourceEntryName(R.drawable.ic_car_library))
        .build(),
)

/**
 * Makes the items the car shows: the fixed nodes with their labels, and
 * one item for each row of the library. Each item either opens a list
 * (browsable) or plays (playable), never both.
 */
internal class BrowseItems(private val labels: BrowseLabels) {
    /** The tabs at the top, the most useful first. A car that shows fewer tabs gets the first ones. */
    fun tabs(): List<MediaItem> = listOf(
        node(MediaId.Library, labels.library, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, icon = labels.libraryIcon),
    )

    /** The lists under the Library tab. */
    fun sections(): List<MediaItem> = listOf(
        node(MediaId.Section(LibraryList.SONGS), labels.songs, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        node(MediaId.Section(LibraryList.ALBUMS), labels.albums, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
        node(MediaId.Section(LibraryList.ARTISTS), labels.artists, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
        node(MediaId.Folders, labels.folders, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
    )

    /** A node that opens a list and is not a row of the library, such as a letter. */
    fun list(id: MediaId, title: String? = null): MediaItem = node(id, title, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    /** An album. It opens its songs. */
    fun album(album: Album): MediaItem = node(
        MediaId.Album(album.id),
        album.title.ifEmpty { labels.unknownAlbum },
        MediaMetadata.MEDIA_TYPE_ALBUM,
        subtitle = album.artist.ifEmpty { null },
        icon = ArtworkAddress.ofAlbum(album.id),
    )

    /** An artist. It opens the songs of the artist. */
    fun artist(artist: Artist): MediaItem =
        node(MediaId.Artist(artist.id), artist.name.ifEmpty { labels.unknownArtist }, MediaMetadata.MEDIA_TYPE_ARTIST)

    /** A folder. It opens the folders and the songs in it. */
    fun folder(folder: Folder): MediaItem =
        node(MediaId.Folder(folder.id), folder.name, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    /** The song with its texts and the id that names its list. It has no address: the car sends back only the id. */
    fun song(song: Song, list: MediaId): MediaItem = MediaItem.Builder()
        .setMediaId(MediaId.SongIn(song.id, list).encode())
        .setMediaMetadata(song.toMediaItem().mediaMetadata)
        .build()
}

/** A node that opens a list. */
private fun node(id: MediaId, title: String?, type: Int, subtitle: String? = null, icon: Uri? = null): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.encode())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setArtworkUri(icon)
                .setMediaType(type)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build(),
        )
        .build()
