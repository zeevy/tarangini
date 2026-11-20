/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Station
import kotlinx.coroutines.flow.Flow

/** The stations a person added by name and address. */
interface StationRepository {
    /** Fires after a station was saved or removed. A list on screen reads again then. */
    fun changes(): Flow<Unit>

    /** The stations the person added, by name. */
    suspend fun userStations(): List<Station>

    /** One station, or null when it is not stored any more. */
    suspend fun station(id: Long): Station?

    /**
     * Saves a station with one stream address and returns its id.
     *
     * @param id the station to change, or null for a new one
     * @throws IllegalArgumentException when the name or the address does not pass
     *   `checkStationName` and `checkStreamAddress`, or another station has the address
     */
    suspend fun save(id: Long?, name: String, streamUrl: String): Long

    /** Removes a station. Its place among the favourites and its play count go with it. */
    suspend fun remove(id: Long)
}
