/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import com.siliconcircuits.swaratarang.core.data.local.entity.PlaylistEntity

/** A playlist with the number of its songs that can be played. */
internal data class PlaylistRow(
    @Embedded val playlist: PlaylistEntity,
    @ColumnInfo(name = "song_count") val songCount: Int,
)
