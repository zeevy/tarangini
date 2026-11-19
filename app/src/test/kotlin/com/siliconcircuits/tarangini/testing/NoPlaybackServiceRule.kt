/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
