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
 * One album of one artist. Two artists can each have an album with the same
 * title. Two spellings of a title with the same [titleKey] are one album.
 * Songs with no album tag share the row with the empty [title].
 */
@Entity(
    tableName = "albums",
    foreignKeys = [
        ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artist_id"]),
    ],
    indices = [Index("artist_id", "title_key", unique = true), Index("title_key")],
)
internal data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "title_key") val titleKey: String,
    @ColumnInfo(name = "artist_id") val artistId: Long,
    val year: Int?,
)
