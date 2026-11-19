/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.siliconcircuits.tarangini.core.data.local.entity.StationEntity
import com.siliconcircuits.tarangini.core.data.local.entity.StationSource
import com.siliconcircuits.tarangini.core.data.local.entity.StationStreamEntity

/** Reads and writes saved stations and their stream URLs. The table is small, so its lists are not paged. */
@Dao
internal abstract class StationDao {
    /**
     * Saves a station with its stream URLs and returns the station id. A
     * station with id 0 is new. The streams given replace the stored ones, in
     * the order of the list.
     */
    @Transaction
    open suspend fun saveStation(station: StationEntity, streams: List<StationStreamEntity>): Long {
        val id = if (station.id == 0L) insertStation(station) else station.id.also { updateStation(station) }
        deleteStreams(id)
        insertStreams(streams.mapIndexed { index, stream -> stream.copy(id = 0, stationId = id, position = index) })
        return id
    }

    @Insert
    protected abstract suspend fun insertStation(station: StationEntity): Long

    @Update
    protected abstract suspend fun updateStation(station: StationEntity)

    @Insert
    protected abstract suspend fun insertStreams(streams: List<StationStreamEntity>)

    @Query("DELETE FROM station_streams WHERE station_id = :stationId")
    protected abstract suspend fun deleteStreams(stationId: Long)

    @Query("SELECT * FROM stations WHERE id = :stationId")
    abstract suspend fun station(stationId: Long): StationEntity?

    @Query("SELECT * FROM stations WHERE radio_browser_uuid = :uuid")
    abstract suspend fun stationByRadioBrowserUuid(uuid: String): StationEntity?

    @Query("SELECT * FROM stations WHERE source = :source ORDER BY name COLLATE NOCASE, id")
    abstract suspend fun stationsBySource(source: StationSource): List<StationEntity>

    @Query("SELECT * FROM station_streams WHERE station_id = :stationId ORDER BY position")
    abstract suspend fun streams(stationId: Long): List<StationStreamEntity>

    /** Removes a station with its streams, its favourite mark and its play count. */
    @Query("DELETE FROM stations WHERE id = :stationId")
    abstract suspend fun deleteStation(stationId: Long)
}
