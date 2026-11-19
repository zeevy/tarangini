/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * One artist. Two spellings with the same [nameKey], such as "ABBA" and
 * "Abba", are one artist; the first spelling saved is kept. Songs with no
 * artist tag share the row with the empty [name]; the screens show their own
 * text for it.
 */
@Entity(
    tableName = "artists",
    indices = [Index("name_key", unique = true)],
)
internal data class ArtistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [name] in the form lists are sorted by. */
    @ColumnInfo(name = "name_key") val nameKey: String,
)
