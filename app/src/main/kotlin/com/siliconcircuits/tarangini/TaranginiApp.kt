/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.app.Application
import androidx.work.Configuration
import com.siliconcircuits.tarangini.core.domain.library.LibraryWatcher
import com.siliconcircuits.tarangini.core.playback.controller.PlaybackScreenWatcher
import com.siliconcircuits.tarangini.core.ui.image.installArtworkLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaranginiApp :
    Application(),
    Configuration.Provider {
    /** The settings of WorkManager, which runs the rescan job. It reads them when it is first used. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    // Hilt can give an Application its objects only through fields that are set after it is made.
    @Suppress("LateinitUsage")
    @Inject
    lateinit var playbackScreenWatcher: PlaybackScreenWatcher

    @Suppress("LateinitUsage")
    @Inject
    lateinit var libraryWatcher: LibraryWatcher

    override fun onCreate() {
        super.onCreate()
        installDebugChecks()
        installArtworkLoader()
        // The playback connection is open while a screen is open, so it must hear of every screen from the first one.
        registerActivityLifecycleCallbacks(playbackScreenWatcher)
        // Every start of the app process brings the library in step: a screen, the playback service or the rescan job.
        libraryWatcher.start()
    }
}
