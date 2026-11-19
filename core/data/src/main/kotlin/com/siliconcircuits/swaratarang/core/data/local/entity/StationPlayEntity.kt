/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** How often and when a saved station was last played. One row per station. */
@Entity(
    tableName = "station_plays",
    foreignKeys = [
        ForeignKey(
            entity = StationEntity::class,
            parentColumns = ["id"],
            childColumns = ["station_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("last_played_at")],
)
internal data class StationPlayEntity(
    @PrimaryKey @ColumnInfo(name = "station_id") val stationId: Long,
    @ColumnInfo(name = "play_count") val playCount: Int,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long,
)
