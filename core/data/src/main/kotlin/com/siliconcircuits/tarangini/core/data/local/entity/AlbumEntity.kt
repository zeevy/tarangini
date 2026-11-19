/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
