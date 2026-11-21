/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import com.siliconcircuits.tarangini.core.data.local.dao.FavouriteDao
import com.siliconcircuits.tarangini.core.data.local.dao.FolderDao
import com.siliconcircuits.tarangini.core.data.local.dao.HistoryDao
import com.siliconcircuits.tarangini.core.data.local.dao.LetterDao
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistDao
import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistFileDao
import com.siliconcircuits.tarangini.core.data.local.dao.QueueDao
import com.siliconcircuits.tarangini.core.data.local.dao.StationDao
import com.siliconcircuits.tarangini.core.data.local.dao.StationStreamDao
import com.siliconcircuits.tarangini.core.data.local.entity.AlbumEntity
import com.siliconcircuits.tarangini.core.data.local.entity.ArtistEntity
import com.siliconcircuits.tarangini.core.data.local.entity.FavouriteSongEntity
import com.siliconcircuits.tarangini.core.data.local.entity.FavouriteStationEntity
import com.siliconcircuits.tarangini.core.data.local.entity.FolderEntity
import com.siliconcircuits.tarangini.core.data.local.entity.PlaylistEntity
import com.siliconcircuits.tarangini.core.data.local.entity.PlaylistItemEntity
import com.siliconcircuits.tarangini.core.data.local.entity.QueueItemEntity
import com.siliconcircuits.tarangini.core.data.local.entity.ResumeStateEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongPlayEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSearchEntity
import com.siliconcircuits.tarangini.core.data.local.entity.StationEntity
import com.siliconcircuits.tarangini.core.data.local.entity.StationPlayEntity
import com.siliconcircuits.tarangini.core.data.local.entity.StationStreamEntity
import com.siliconcircuits.tarangini.core.data.local.migration.SearchTableWholeWords

/**
 * The one database of the app. A change to an entity needs a higher
 * [version], a migration and a migration test; the schema of every version
 * is written to the `schemas` folder of the module and committed.
 */
@Database(
    entities = [
        AlbumEntity::class,
        ArtistEntity::class,
        FavouriteSongEntity::class,
        FavouriteStationEntity::class,
        FolderEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        QueueItemEntity::class,
        ResumeStateEntity::class,
        SongEntity::class,
        SongPlayEntity::class,
        SongSearchEntity::class,
        StationEntity::class,
        StationPlayEntity::class,
        StationStreamEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
internal abstract class TaranginiDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao

    abstract fun folderDao(): FolderDao

    abstract fun historyDao(): HistoryDao

    abstract fun libraryDao(): LibraryDao

    abstract fun letterDao(): LetterDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun playlistFileDao(): PlaylistFileDao

    abstract fun queueDao(): QueueDao

    abstract fun stationDao(): StationDao

    abstract fun stationStreamDao(): StationStreamDao

    companion object {
        const val FILE_NAME = "tarangini.db"

        /** Every step from an older version to this one. A new version adds its step here. */
        val MIGRATIONS: List<Migration> = listOf(SearchTableWholeWords)
    }
}
