/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import com.siliconcircuits.tarangini.core.data.local.TaranginiDatabase
import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistFileDao
import com.siliconcircuits.tarangini.core.data.playlist.DefaultPlaylistFiles
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistFiles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** What the import and the export of playlist files need. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaylistFilesModule {
    @Binds
    abstract fun playlistFiles(files: DefaultPlaylistFiles): PlaylistFiles

    companion object {
        @Provides
        fun playlistFileDao(database: TaranginiDatabase): PlaylistFileDao = database.playlistFileDao()
    }
}
