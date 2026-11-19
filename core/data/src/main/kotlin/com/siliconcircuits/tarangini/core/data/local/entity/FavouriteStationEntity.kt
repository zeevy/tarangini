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

/** Marks a station as a favourite. The user sets the order with [position]. */
@Entity(
    tableName = "favourite_stations",
    foreignKeys = [
        ForeignKey(
            entity = StationEntity::class,
            parentColumns = ["id"],
            childColumns = ["station_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("position")],
)
internal data class FavouriteStationEntity(
    @PrimaryKey @ColumnInfo(name = "station_id") val stationId: Long,
    val position: Int,
)
