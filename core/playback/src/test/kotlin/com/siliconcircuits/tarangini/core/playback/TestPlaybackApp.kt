/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback

import android.app.Application
import com.siliconcircuits.tarangini.core.domain.playback.ResumeStore
import com.siliconcircuits.tarangini.core.playback.service.PlaybackService
import com.siliconcircuits.tarangini.core.testing.playback.FakeResumeStore
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * The application of a test that starts the playback service. It hands the
 * service what the real app hands it through Hilt, with a store a test can
 * fill and read.
 */
internal class TestPlaybackApp :
    Application(),
    GeneratedComponentManager<Any> {
    val resumeStore = FakeResumeStore()
    private val scope = MainScope()

    override fun generatedComponent(): Any = object : PlaybackService.Dependencies, GeneratedComponent {
        override fun resumeStore(): ResumeStore = resumeStore

        override fun playbackScope(): CoroutineScope = scope
    }
}
