/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.StationStreamDao
import com.siliconcircuits.tarangini.core.domain.repository.StationStreams
import javax.inject.Inject

/** [StationStreams] on the stations tables. */
internal class DefaultStationStreams @Inject constructor(private val stations: StationStreamDao) : StationStreams {
    override suspend fun ofStationWith(address: String): List<String> = stations.streamUrlsOfStationWith(address)
}
