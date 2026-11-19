/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.testing

import android.content.ComponentName
import androidx.activity.ComponentActivity
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Lets a Compose test start a plain activity under Robolectric. The app
 * manifest does not list that activity, so it is added to the fake package
 * manager before the Compose rule starts it.
 */
class RegisterComponentActivityRule : TestWatcher() {
    override fun starting(description: Description) {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app.packageManager)
            .addActivityIfNotPresent(ComponentName(app.packageName, ComponentActivity::class.java.name))
    }
}
