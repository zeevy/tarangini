/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.app.Application
import com.siliconcircuits.tarangini.core.playback.controller.PlaybackScreenWatcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaranginiApp : Application() {
    // Hilt can give an Application its objects only through fields that are set after it is made.
    @Suppress("LateinitUsage")
    @Inject
    lateinit var playbackScreenWatcher: PlaybackScreenWatcher

    override fun onCreate() {
        super.onCreate()
        // The playback connection is open while a screen is open, so it must hear of every screen from the first one.
        registerActivityLifecycleCallbacks(playbackScreenWatcher)
    }
}
