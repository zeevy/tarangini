/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.core.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.FtsOptions
import androidx.room3.PrimaryKey

/**
 * The full text search table for songs. The row id is the song id. The
 * library DAO writes it in the same transaction as the song, so the two
 * never differ.
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "song_search")
internal data class SongSearchEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
)
