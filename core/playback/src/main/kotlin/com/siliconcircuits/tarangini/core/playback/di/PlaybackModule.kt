/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

// This module is where the playback scope gets its dispatcher, so that everything else can have it injected.
@file:Suppress("ForbiddenImport")

package com.siliconcircuits.tarangini.core.playback.di

import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.domain.station.StreamTester
import com.siliconcircuits.tarangini.core.playback.controller.MediaControllerConnector
import com.siliconcircuits.tarangini.core.playback.controller.SessionConnector
import com.siliconcircuits.tarangini.core.playback.controller.SessionPlaybackController
import com.siliconcircuits.tarangini.core.playback.controller.SessionPlaybackQueue
import com.siliconcircuits.tarangini.core.playback.stream.DefaultStreamTester
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlaybackModule {
    @Binds
    abstract fun playbackController(controller: SessionPlaybackController): PlaybackController

    @Binds
    abstract fun playbackQueue(queue: SessionPlaybackQueue): PlaybackQueue

    @Binds
    abstract fun sessionConnector(connector: MediaControllerConnector): SessionConnector

    @Binds
    abstract fun streamTester(tester: DefaultStreamTester): StreamTester

    companion object {
        /** A media controller may be used only on the main thread, so the scope runs there. */
        @Provides
        @Singleton
        @PlaybackCoroutineScope
        fun playbackScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        @Provides
        @StreamIoDispatcher
        fun streamIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
