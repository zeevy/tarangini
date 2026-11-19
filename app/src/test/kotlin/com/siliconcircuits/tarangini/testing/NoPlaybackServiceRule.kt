/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.testing

import android.content.ComponentName
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Keeps the playback service out of a screen test. The app connects to the service when a screen
 * opens. Robolectric answers such a connection with an empty one, which Media3 cannot handle, so
 * the test tells Robolectric that the service cannot be reached.
 */
class NoPlaybackServiceRule : TestWatcher() {
    override fun starting(description: Description) {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).declareComponentUnbindable(ComponentName(app.packageName, PLAYBACK_SERVICE))
    }

    private companion object {
        const val PLAYBACK_SERVICE = "com.siliconcircuits.tarangini.core.playback.service.PlaybackService"
    }
}
