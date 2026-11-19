/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.siliconcircuits.swaratarang.core.data.local.SwaraTarangDatabase
import com.siliconcircuits.swaratarang.core.data.local.dao.FavouriteDao
import com.siliconcircuits.swaratarang.core.data.local.dao.HistoryDao
import com.siliconcircuits.swaratarang.core.data.local.dao.LibraryDao
import com.siliconcircuits.swaratarang.core.data.local.dao.PlaylistDao
import com.siliconcircuits.swaratarang.core.data.local.dao.QueueDao
import com.siliconcircuits.swaratarang.core.data.local.dao.StationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): SwaraTarangDatabase =
        Room.databaseBuilder<SwaraTarangDatabase>(context, SwaraTarangDatabase.FILE_NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides
    fun favouriteDao(database: SwaraTarangDatabase): FavouriteDao = database.favouriteDao()

    @Provides
    fun historyDao(database: SwaraTarangDatabase): HistoryDao = database.historyDao()

    @Provides
    fun libraryDao(database: SwaraTarangDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun playlistDao(database: SwaraTarangDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun queueDao(database: SwaraTarangDatabase): QueueDao = database.queueDao()

    @Provides
    fun stationDao(database: SwaraTarangDatabase): StationDao = database.stationDao()
}
