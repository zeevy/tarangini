/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One saved radio station: a favourite, a station the user typed in, or one
 * played lately. The row is our own copy, so the station still plays when the
 * directory changes or drops it.
 */
@Entity(
    tableName = "stations",
    indices = [Index("radio_browser_uuid", unique = true)],
)
internal data class StationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: StationSource,
    /** The id of the station in the Radio Browser directory, or null for a station the user typed in. */
    @ColumnInfo(name = "radio_browser_uuid") val radioBrowserUuid: String?,
    val name: String,
    val homepage: String?,
    @ColumnInfo(name = "country_code") val countryCode: String?,
    val language: String?,
    val tags: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
