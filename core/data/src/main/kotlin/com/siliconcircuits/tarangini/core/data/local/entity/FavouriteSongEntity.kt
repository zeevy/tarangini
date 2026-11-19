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

/** Marks a song as a favourite. The row goes away with its song. */
@Entity(
    tableName = "favourite_songs",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("added_at")],
)
internal data class FavouriteSongEntity(
    @PrimaryKey @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
