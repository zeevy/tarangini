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
 * Where playback stopped. The table has one row at most, with the id
 * [SINGLE_ROW_ID]. [stationId] is set when a station was playing, and the
 * saved queue is then not used.
 */
@Entity(
    tableName = "resume_state",
    foreignKeys = [
        ForeignKey(
            entity = StationEntity::class,
            parentColumns = ["id"],
            childColumns = ["station_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("station_id")],
)
internal data class ResumeStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    /** The [QueueItemEntity.position] of the song that was playing. */
    @ColumnInfo(name = "queue_position") val queuePosition: Int,
    @ColumnInfo(name = "position_ms") val positionMs: Long,
    @ColumnInfo(name = "station_id") val stationId: Long?,
    val shuffle: Boolean,
    @ColumnInfo(name = "repeat_mode") val repeatMode: StoredRepeatMode,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
