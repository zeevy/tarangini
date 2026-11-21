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
import com.siliconcircuits.tarangini.core.data.local.SEARCH_TOKEN_CHARS

/**
 * The full text search table for songs. The row id is the song id. The
 * library DAO writes it in the same transaction as the song, so the two
 * never differ.
 *
 * A word of an Indian script stays whole ([SEARCH_TOKEN_CHARS]), so a search for the start of a
 * word does not find a word with another vowel.
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, tokenizerArgs = [SEARCH_TOKEN_CHARS])
@Entity(tableName = "song_search")
internal data class SongSearchEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
)
