/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import com.siliconcircuits.swaratarang.core.data.local.entity.SongEntity

/** A song with the names a list shows next to it. */
internal data class SongRow(
    @Embedded val song: SongEntity,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "album_title") val albumTitle: String,
)
