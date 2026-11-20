/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query

/** Reads of the stream URLs that start from a URL, for the player. */
@Dao
internal interface StationStreamDao {
    /**
     * The stream URLs of the station that has [url], in their order. When two stations have the
     * same URL, the one stored first is taken. The table has a few rows per station the user keeps,
     * so the read by URL needs no index.
     */
    @Query(
        "SELECT url FROM station_streams WHERE station_id = " +
            "(SELECT station_id FROM station_streams WHERE url = :url ORDER BY station_id LIMIT 1) " +
            "ORDER BY position",
    )
    suspend fun streamUrlsOfStationWith(url: String): List<String>

    /**
     * The station names for these stream URLs. When two stations have the same URL, the one
     * stored first comes last. Give at most 500 URLs: SQLite limits the values of one statement.
     */
    @Query(
        "SELECT ss.url AS url, s.name AS name FROM station_streams ss " +
            "JOIN stations s ON s.id = ss.station_id WHERE ss.url IN (:urls) ORDER BY s.id DESC",
    )
    suspend fun namesByUrl(urls: List<String>): List<StreamName>
}

/** The name of the station that plays from [url]. */
internal data class StreamName(val url: String, val name: String)
