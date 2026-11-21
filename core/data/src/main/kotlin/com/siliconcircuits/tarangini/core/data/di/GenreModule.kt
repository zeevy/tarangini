/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import com.siliconcircuits.tarangini.core.data.local.TaranginiDatabase
import com.siliconcircuits.tarangini.core.data.local.dao.GenreDao
import com.siliconcircuits.tarangini.core.data.repository.DefaultGenreRepository
import com.siliconcircuits.tarangini.core.domain.repository.GenreRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** What the genre lists need. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class GenreModule {
    @Binds
    abstract fun genreRepository(repository: DefaultGenreRepository): GenreRepository

    companion object {
        @Provides
        fun genreDao(database: TaranginiDatabase): GenreDao = database.genreDao()
    }
}
