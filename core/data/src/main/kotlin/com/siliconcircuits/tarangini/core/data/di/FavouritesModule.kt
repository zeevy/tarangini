/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import com.siliconcircuits.tarangini.core.data.repository.DefaultFavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Gives the screens and the playback service the favourites. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class FavouritesModule {
    @Binds
    abstract fun favouriteRepository(repository: DefaultFavouriteRepository): FavouriteRepository
}
