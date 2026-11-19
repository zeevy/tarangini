/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.siliconcircuits.swaratarang.core.data.local.dao.FavouriteDao
import com.siliconcircuits.swaratarang.core.data.local.dao.HistoryDao
import com.siliconcircuits.swaratarang.core.data.local.dao.LibraryDao
import com.siliconcircuits.swaratarang.core.data.local.dao.PlaylistDao
import com.siliconcircuits.swaratarang.core.data.local.dao.QueueDao
import com.siliconcircuits.swaratarang.core.data.local.dao.StationDao
import com.siliconcircuits.swaratarang.core.data.local.entity.AlbumEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.ArtistEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.FavouriteSongEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.FavouriteStationEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.FolderEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.PlaylistEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.PlaylistItemEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.QueueItemEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.ResumeStateEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.SongEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.SongPlayEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.SongSearchEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.StationEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.StationPlayEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.StationStreamEntity

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
    version = 1,
    exportSchema = true,
)
internal abstract class SwaraTarangDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao

    abstract fun historyDao(): HistoryDao

    abstract fun libraryDao(): LibraryDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun queueDao(): QueueDao

    abstract fun stationDao(): StationDao

    companion object {
        const val FILE_NAME = "swara-tarang.db"
    }
}
