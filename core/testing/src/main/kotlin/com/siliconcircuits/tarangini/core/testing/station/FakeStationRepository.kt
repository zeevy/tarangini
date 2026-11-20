/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.station

import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.domain.repository.StationRepository
import com.siliconcircuits.tarangini.core.domain.station.checkStationName
import com.siliconcircuits.tarangini.core.domain.station.checkStreamAddress
import com.siliconcircuits.tarangini.core.domain.station.cleanStationName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * [StationRepository] over a list a test fills and reads.
 *
 * @property failure when set, every call fails with it
 */
class FakeStationRepository(var stations: List<Station> = emptyList(), var failure: Exception? = null) :
    StationRepository {
    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    override fun changes(): Flow<Unit> = changed

    override suspend fun userStations(): List<Station> = checked { stations.sortedBy { it.name.lowercase() } }

    override suspend fun station(id: Long): Station? = checked { stations.firstOrNull { it.id == id } }

    override suspend fun save(id: Long?, name: String, streamUrl: String): Long = checked {
        require(checkStationName(name) == null && checkStreamAddress(streamUrl) == null)
        require(stations.none { it.id != id && streamUrl.trim() in it.streamUrls })
        val saved =
            Station(id ?: ((stations.maxOfOrNull { it.id } ?: 0) + 1), cleanStationName(name), listOf(streamUrl.trim()))
        stations = stations.filterNot { it.id == saved.id } + saved
        changed.tryEmit(Unit)
        saved.id
    }

    override suspend fun remove(id: Long) = checked {
        stations = stations.filterNot { it.id == id }
        changed.tryEmit(Unit)
        Unit
    }

    private inline fun <T> checked(block: () -> T): T {
        failure?.let { throw it }
        return block()
    }
}
