/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One song. [source] and [sourceId] together name the file where it comes
 * from, so a song from another kind of storage can use the same table.
 *
 * A song whose file is gone is not removed at once: [missingSince] is set and
 * the lists leave it out. Its favourite, playlist and play count rows stay,
 * so a memory card that is taken out and put back loses nothing.
 */
@Entity(
    tableName = "songs",
    foreignKeys = [
        ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artist_id"]),
        ForeignKey(entity = AlbumEntity::class, parentColumns = ["id"], childColumns = ["album_id"]),
        ForeignKey(entity = FolderEntity::class, parentColumns = ["id"], childColumns = ["folder_id"]),
    ],
    indices = [
        Index("source", "source_id", unique = true),
        Index("title_key"),
        Index("artist_id", "title_key"),
        Index("album_id", "disc_number", "track_number"),
        Index("folder_id", "file_name"),
        Index("date_added"),
        Index("duration_ms"),
    ],
)
internal data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: SongSource,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val title: String,
    @ColumnInfo(name = "title_key") val titleKey: String,
    @ColumnInfo(name = "artist_id") val artistId: Long,
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "folder_id") val folderId: Long?,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "track_number") val trackNumber: Int?,
    @ColumnInfo(name = "disc_number") val discNumber: Int?,
    val year: Int?,
    /** When the file was added to the phone, in milliseconds since 1970 UTC. */
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    /** When a scan first found the file gone, or null while the file is there. */
    @ColumnInfo(name = "missing_since") val missingSince: Long? = null,
)
