/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded

/** One place in a playlist with its song. [itemId] names the place, since a song can be there twice. */
internal data class PlaylistSongRow(@ColumnInfo(name = "item_id") val itemId: Long, @Embedded val row: SongRow)
