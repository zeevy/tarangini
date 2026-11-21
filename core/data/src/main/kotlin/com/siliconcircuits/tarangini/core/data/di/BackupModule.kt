/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import com.siliconcircuits.tarangini.core.data.backup.DefaultBackupFiles
import com.siliconcircuits.tarangini.core.data.local.TaranginiDatabase
import com.siliconcircuits.tarangini.core.data.local.dao.BackupDao
import com.siliconcircuits.tarangini.core.domain.repository.BackupFiles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** What a backup to a file needs. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupModule {
    @Binds
    abstract fun backupFiles(files: DefaultBackupFiles): BackupFiles

    companion object {
        @Provides
        fun backupDao(database: TaranginiDatabase): BackupDao = database.backupDao()
    }
}
