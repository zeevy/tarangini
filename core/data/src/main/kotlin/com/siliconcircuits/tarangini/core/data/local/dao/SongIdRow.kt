/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.ColumnInfo

/** The id of a song row and the id its file has at its source. */
internal data class SongIdRow(val id: Long, @ColumnInfo(name = "source_id") val sourceId: String)
