/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.controller

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
