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

/** One stream URL of a station. The player tries them in the order of [position]. */
@Entity(
    tableName = "station_streams",
    foreignKeys = [
        ForeignKey(
            entity = StationEntity::class,
            parentColumns = ["id"],
            childColumns = ["station_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("station_id", "position", unique = true)],
)
internal data class StationStreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "station_id") val stationId: Long,
    val position: Int,
    val url: String,
    val codec: String?,
    @ColumnInfo(name = "bitrate_kbps") val bitrateKbps: Int?,
)
