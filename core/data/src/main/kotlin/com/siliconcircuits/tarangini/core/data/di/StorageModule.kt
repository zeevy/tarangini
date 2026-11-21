/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import com.siliconcircuits.tarangini.core.data.mediastore.AudioFileOpener
import com.siliconcircuits.tarangini.core.data.mediastore.ContentAudioFileOpener
import com.siliconcircuits.tarangini.core.data.mediastore.LocalStorageSource
import com.siliconcircuits.tarangini.core.domain.premium.StorageSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

/**
 * Puts the files of the phone into the set of storage sources. The set is in both builds; the
 * premium module adds its sources with `@IntoSet`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class StorageModule {
    @Multibinds
    abstract fun storageSources(): Set<StorageSource>

    @Binds
    @IntoSet
    abstract fun localStorage(source: LocalStorageSource): StorageSource

    @Binds
    abstract fun audioFileOpener(opener: ContentAudioFileOpener): AudioFileOpener
}
