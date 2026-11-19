/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import com.siliconcircuits.tarangini.core.data.local.entity.AlbumEntity

/** An album with its artist name and the number of its songs that can be played. */
internal data class AlbumRow(
    @Embedded val album: AlbumEntity,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "song_count") val songCount: Int,
)
