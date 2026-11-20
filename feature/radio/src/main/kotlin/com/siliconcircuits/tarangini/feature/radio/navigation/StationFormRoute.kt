/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio.navigation

import kotlinx.serialization.Serializable

/**
 * The form that adds or changes a station.
 *
 * @property stationId the station to change, or 0 for a new one
 */
@Serializable
internal data class StationFormRoute(val stationId: Long = 0)
