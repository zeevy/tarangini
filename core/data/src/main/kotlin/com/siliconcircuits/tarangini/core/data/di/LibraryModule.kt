/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import com.siliconcircuits.tarangini.core.data.mediastore.AudioFileSource
import com.siliconcircuits.tarangini.core.data.mediastore.DefaultLibraryScanner
import com.siliconcircuits.tarangini.core.data.mediastore.DefaultLibraryWatcher
import com.siliconcircuits.tarangini.core.data.mediastore.MediaStoreAudioSource
import com.siliconcircuits.tarangini.core.data.mediastore.TimeSource
import com.siliconcircuits.tarangini.core.data.repository.DefaultLibraryRepository
import com.siliconcircuits.tarangini.core.data.repository.DefaultSettingsRepository
import com.siliconcircuits.tarangini.core.data.work.RescanSchedule
import com.siliconcircuits.tarangini.core.data.work.WorkManagerRescanSchedule
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.library.LibraryWatcher
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LibraryModule {
    @Binds
    abstract fun libraryScanner(scanner: DefaultLibraryScanner): LibraryScanner

    @Binds
    abstract fun audioFileSource(source: MediaStoreAudioSource): AudioFileSource

    @Binds
    abstract fun libraryWatcher(watcher: DefaultLibraryWatcher): LibraryWatcher

    @Binds
    abstract fun libraryRepository(repository: DefaultLibraryRepository): LibraryRepository

    @Binds
    abstract fun settingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    abstract fun rescanSchedule(schedule: WorkManagerRescanSchedule): RescanSchedule

    companion object {
        @Provides
        fun timeSource(): TimeSource = TimeSource { System.currentTimeMillis() }
    }
}
