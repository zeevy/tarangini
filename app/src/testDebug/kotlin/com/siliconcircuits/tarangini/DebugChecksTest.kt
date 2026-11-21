/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.os.StrictMode
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DebugChecksTest {
    @Test
    fun `a debug build starts with the StrictMode checks on`() {
        // Asking for the application starts it, as a launch of the app does.
        RuntimeEnvironment.getApplication()

        assertNotEquals(StrictMode.ThreadPolicy.LAX.toString(), StrictMode.getThreadPolicy().toString())
        assertNotEquals(StrictMode.VmPolicy.LAX.toString(), StrictMode.getVmPolicy().toString())
    }
}
