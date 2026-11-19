/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.testing

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
