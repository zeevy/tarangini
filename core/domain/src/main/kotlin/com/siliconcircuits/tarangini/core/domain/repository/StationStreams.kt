/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

/** The stream addresses of the stored stations, for the player. */
interface StationStreams {
    /**
     * Every stream address of the station that has [address], in the order to try them.
     * The list is empty when no stored station has the address.
     */
    suspend fun ofStationWith(address: String): List<String>
}
