/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded

/** One place in a playlist with its song. [itemId] names the place, since a song can be there twice. */
internal data class PlaylistSongRow(@ColumnInfo(name = "item_id") val itemId: Long, @Embedded val row: SongRow)
