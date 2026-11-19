/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang

import android.app.Application
import com.siliconcircuits.swaratarang.core.playback.controller.PlaybackScreenWatcher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SwaraTarangApp : Application() {
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
