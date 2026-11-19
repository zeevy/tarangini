/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import android.app.Activity
import android.app.Application
import android.os.Bundle
import javax.inject.Inject

/**
 * Tells the playback connection when the app has a screen open.
 *
 * The app registers it once, when the app starts and before any screen is made.
 */
class PlaybackScreenWatcher @Inject internal constructor(private val link: PlayerLink) :
    Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = link.onScreenOpened()

    override fun onActivityDestroyed(activity: Activity) =
        link.onScreenClosed(comesBack = activity.isChangingConfigurations)

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
