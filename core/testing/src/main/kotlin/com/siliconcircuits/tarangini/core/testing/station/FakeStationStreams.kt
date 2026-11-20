/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.station

import com.siliconcircuits.tarangini.core.domain.repository.StationStreams

/**
 * [StationStreams] over lists a test fills: one list of addresses per station.
 *
 * @property failure when set, every read fails with it
 * @property names the station name per stream address
 * @property onRead runs at the start of every read, for a test of what happens in the meantime
 */
class FakeStationStreams(
    var stations: List<List<String>> = emptyList(),
    var failure: Exception? = null,
    var onRead: () -> Unit = {},
    var names: Map<String, String> = emptyMap(),
) : StationStreams {
    override suspend fun ofStationWith(address: String): List<String> {
        onRead()
        failure?.let { throw it }
        return stations.firstOrNull { address in it }.orEmpty()
    }

    override suspend fun namesByAddress(addresses: List<String>): Map<String, String> {
        failure?.let { throw it }
        return names.filterKeys { it in addresses }
    }
}
