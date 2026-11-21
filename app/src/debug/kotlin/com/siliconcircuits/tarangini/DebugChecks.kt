/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.os.StrictMode

/**
 * Switches on the checks of a debug build: Android writes a line to the log when the app reads a
 * file or the network on the main thread, or leaves a file, a cursor, a receiver or a screen
 * behind. The checks only log. A library that trips one must not end the app of a developer.
 *
 * A debug build also has LeakCanary, which starts by itself and shows a notice for a leak.
 * The release build has an empty function of the same name and neither check.
 */
internal fun installDebugChecks() {
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedClosableObjects()
            .detectLeakedSqlLiteObjects()
            .detectLeakedRegistrationObjects()
            .detectActivityLeaks()
            .penaltyLog()
            .build(),
    )
}
