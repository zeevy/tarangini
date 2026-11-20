/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.di

import com.siliconcircuits.tarangini.core.data.mediastore.DefaultAudioAccess
import com.siliconcircuits.tarangini.core.data.repository.DefaultLibraryLetters
import com.siliconcircuits.tarangini.core.domain.repository.AudioAccess
import com.siliconcircuits.tarangini.core.domain.repository.LibraryLetters
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * What the playback service needs for the list the car shows: the first
 * letters of the lists, and the state of the audio permission.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LettersModule {
    @Binds
    abstract fun libraryLetters(letters: DefaultLibraryLetters): LibraryLetters

    @Binds
    abstract fun audioAccess(access: DefaultAudioAccess): AudioAccess
}
