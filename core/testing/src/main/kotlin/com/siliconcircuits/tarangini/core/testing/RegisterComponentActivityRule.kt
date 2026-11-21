/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing

import android.content.ComponentName
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Lets a Compose test start a plain activity under Robolectric. The manifest of a library or of
 * the app does not list that activity, so it is added to the fake package manager before the
 * Compose rule starts it. Put it outside the Compose rule in a rule chain.
 */
class RegisterComponentActivityRule : TestWatcher() {
    override fun starting(description: Description) {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app.packageManager)
            .addActivityIfNotPresent(ComponentName(app.packageName, COMPONENT_ACTIVITY))
    }

    private companion object {
        // By name, so the rule needs no activity library; every test that starts it has one.
        const val COMPONENT_ACTIVITY = "androidx.activity.ComponentActivity"
    }
}
