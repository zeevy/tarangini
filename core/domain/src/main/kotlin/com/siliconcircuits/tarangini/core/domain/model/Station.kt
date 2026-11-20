/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * A radio station the app keeps.
 *
 * @property id the id of the stored station
 * @property name the name the person gave it
 * @property streamUrls its stream addresses, in the order to try them. There is at least one
 */
data class Station(val id: Long, val name: String, val streamUrls: List<String>)
