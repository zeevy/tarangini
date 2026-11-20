/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.StationDao
import com.siliconcircuits.tarangini.core.data.local.entity.StationEntity
import com.siliconcircuits.tarangini.core.data.local.entity.StationSource
import com.siliconcircuits.tarangini.core.data.local.entity.StationStreamEntity
import com.siliconcircuits.tarangini.core.data.mediastore.TimeSource
import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.domain.repository.StationRepository
import com.siliconcircuits.tarangini.core.domain.station.checkStationName
import com.siliconcircuits.tarangini.core.domain.station.checkStreamAddress
import com.siliconcircuits.tarangini.core.domain.station.cleanStationName
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** [StationRepository] on the stations tables. One object for the app, so every screen hears a change. */
@Singleton
internal class DefaultStationRepository @Inject constructor(
    private val stations: StationDao,
    private val time: TimeSource,
) : StationRepository {
    // One waiting signal is enough: a reader reads everything again.
    private val changed =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun changes(): Flow<Unit> = changed

    override suspend fun userStations(): List<Station> =
        stations.stationsBySource(StationSource.USER).map { it.withStreams() }

    override suspend fun station(id: Long): Station? = stations.station(id)?.withStreams()

    override suspend fun save(id: Long?, name: String, streamUrl: String): Long {
        // The form checks first. This is the last gate before the database, for every caller.
        require(checkStationName(name) == null) { "The station name cannot be saved" }
        require(checkStreamAddress(streamUrl) == null) { "The stream address cannot be saved" }
        val url = streamUrl.trim()
        val old = id?.let { stations.station(it) }
        // The form shows only stations the person added. A station of the directory keeps its own streams.
        require(old == null || old.source == StationSource.USER) { "Only a station the person added can be changed" }
        // One address, one station: the player finds the name of a stream by its address.
        require(userStations().none { it.id != old?.id && url in it.streamUrls }) { "Another station has this address" }
        val now = time.nowMs()
        val station = old?.copy(name = cleanStationName(name), updatedAt = now)
            ?: StationEntity(
                source = StationSource.USER,
                radioBrowserUuid = null,
                name = cleanStationName(name),
                homepage = null,
                countryCode = null,
                language = null,
                tags = null,
                createdAt = now,
                updatedAt = now,
            )
        val stream =
            StationStreamEntity(stationId = 0, position = 0, url = url, codec = null, bitrateKbps = null)
        return stations.saveStation(station, listOf(stream)).also { changed.tryEmit(Unit) }
    }

    override suspend fun remove(id: Long) {
        stations.deleteStation(id)
        changed.tryEmit(Unit)
    }

    private suspend fun StationEntity.withStreams() = Station(id, name, stations.streams(id).map { it.url })
}
