/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A genre as the tags of the files name it. The genre with the empty name holds the songs with no genre tag. */
@Entity(
    tableName = "genres",
    indices = [Index("name_key", unique = true)],
)
internal data class GenreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** [name] in the form lists are sorted by. */
    @ColumnInfo(name = "name_key") val nameKey: String,
)

/**
 * One genre of one song. A song has at least one row once the scan has read its genre tag, so a
 * song with no row is one whose tag was never read.
 */
@Entity(
    tableName = "song_genres",
    primaryKeys = ["song_id", "genre_id"],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["song_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GenreEntity::class,
            parentColumns = ["id"],
            childColumns = ["genre_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("genre_id")],
)
internal data class SongGenreEntity(
    @ColumnInfo(name = "song_id") val songId: Long,
    @ColumnInfo(name = "genre_id") val genreId: Long,
)
