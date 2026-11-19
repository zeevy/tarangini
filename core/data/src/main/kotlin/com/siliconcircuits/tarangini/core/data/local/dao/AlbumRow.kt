/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
