/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.siliconcircuits.tarangini.core.data.local.TaranginiDatabase
import com.siliconcircuits.tarangini.core.data.local.dao.FavouriteDao
import com.siliconcircuits.tarangini.core.data.local.dao.HistoryDao
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistDao
import com.siliconcircuits.tarangini.core.data.local.dao.QueueDao
import com.siliconcircuits.tarangini.core.data.local.dao.StationDao
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
    fun database(@ApplicationContext context: Context): TaranginiDatabase =
        Room.databaseBuilder<TaranginiDatabase>(context, TaranginiDatabase.FILE_NAME)
            .setDriver(AndroidSQLiteDriver())
            .build()

    @Provides
    fun favouriteDao(database: TaranginiDatabase): FavouriteDao = database.favouriteDao()

    @Provides
    fun historyDao(database: TaranginiDatabase): HistoryDao = database.historyDao()

    @Provides
    fun libraryDao(database: TaranginiDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun playlistDao(database: TaranginiDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun queueDao(database: TaranginiDatabase): QueueDao = database.queueDao()

    @Provides
    fun stationDao(database: TaranginiDatabase): StationDao = database.stationDao()
}
