/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// This module is where the playback scope gets its dispatcher, so that everything else can have it injected.
@file:Suppress("ForbiddenImport")

package com.siliconcircuits.tarangini.core.playback.di

import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.playback.controller.MediaControllerConnector
import com.siliconcircuits.tarangini.core.playback.controller.SessionConnector
import com.siliconcircuits.tarangini.core.playback.controller.SessionPlaybackController
import com.siliconcircuits.tarangini.core.playback.controller.SessionPlaybackQueue
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
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

    companion object {
        /** A media controller may be used only on the main thread, so the scope runs there. */
        @Provides
        @Singleton
        @PlaybackCoroutineScope
        fun playbackScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}
