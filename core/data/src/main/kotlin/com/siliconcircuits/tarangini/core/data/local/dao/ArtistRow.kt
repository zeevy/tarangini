/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.ColumnInfo
import androidx.room3.Embedded
import com.siliconcircuits.tarangini.core.data.local.entity.ArtistEntity

/** An artist with the number of their songs that can be played. */
internal data class ArtistRow(@Embedded val artist: ArtistEntity, @ColumnInfo(name = "song_count") val songCount: Int)
