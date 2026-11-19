/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
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
