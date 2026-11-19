/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.siliconcircuits.swaratarang.core.data.local.entity.PlaylistEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.SongEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.SongSource
import com.siliconcircuits.swaratarang.core.data.local.entity.StationEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.StationSource
import com.siliconcircuits.swaratarang.core.data.local.entity.StationStreamEntity
import org.robolectric.RuntimeEnvironment

/** A new, empty database in memory. */
internal fun testDatabase(): SwaraTarangDatabase =
    Room.inMemoryDatabaseBuilder<SwaraTarangDatabase>(RuntimeEnvironment.getApplication())
        .setDriver(AndroidSQLiteDriver())
        .build()

/** A song row for the artist and album with these ids. */
internal fun songEntity(title: String, sourceId: String, artistId: Long, albumId: Long) = SongEntity(
    source = SongSource.LOCAL,
    sourceId = sourceId,
    title = title,
    titleKey = sortKey(title),
    artistId = artistId,
    albumId = albumId,
    folderId = null,
    fileName = "$title.mp3",
    mimeType = "audio/mpeg",
    durationMs = 180_000,
    sizeBytes = 1,
    trackNumber = null,
    discNumber = null,
    year = null,
    dateAdded = 0,
    dateModified = 0,
)

/** Saves a song with its artist and album, the way a scan does, and returns the song id. */
@Suppress("LongParameterList") // Each test sets only the columns it is about.
internal suspend fun SwaraTarangDatabase.addSong(
    title: String,
    artist: String = "Artist",
    album: String = "Album",
    sourceId: String = title,
    folderId: Long? = null,
    disc: Int? = null,
    track: Int? = null,
    durationMs: Long = 180_000,
    dateAdded: Long = 0,
): Long {
    val library = libraryDao()
    val artistId = library.saveArtist(artist, sortKey(artist))
    val albumId = library.saveAlbum(album, sortKey(album), artistId, year = null)
    return library.saveSong(
        songEntity(title, sourceId, artistId, albumId).copy(
            folderId = folderId,
            discNumber = disc,
            trackNumber = track,
            durationMs = durationMs,
            dateAdded = dateAdded,
        ),
    )
}

/** Saves a station with one stream per entry of [urls] and returns the station id. */
internal suspend fun SwaraTarangDatabase.addStation(
    name: String,
    uuid: String? = null,
    urls: List<String> = listOf("https://example.org/$name"),
): Long = stationDao().saveStation(
    StationEntity(
        source = if (uuid == null) StationSource.USER else StationSource.RADIO_BROWSER,
        radioBrowserUuid = uuid,
        name = name,
        homepage = null,
        countryCode = null,
        language = null,
        tags = null,
        createdAt = 0,
        updatedAt = 0,
    ),
    urls.map { StationStreamEntity(stationId = 0, position = 0, url = it, codec = null, bitrateKbps = null) },
)

/** Makes an empty playlist and returns its id. */
internal suspend fun SwaraTarangDatabase.addPlaylist(name: String): Long =
    playlistDao().insertPlaylist(PlaylistEntity(name = name, createdAt = 0, updatedAt = 0))
