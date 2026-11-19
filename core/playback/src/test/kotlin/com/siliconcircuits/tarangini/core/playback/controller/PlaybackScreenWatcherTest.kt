/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackScreenWatcherTest {
    private val connector = FakeSessionConnector()
    private val watcher = PlaybackScreenWatcher(PlayerLink(connector))
    private val screen = Robolectric.buildActivity(Activity::class.java)

    @Test
    fun `a screen that is made opens the connection and a screen that is closed releases it`() {
        watcher.onActivityCreated(screen.get(), null)
        assertEquals(1, connector.attemptCount)
        assertFalse(connector.lastReleased)

        watcher.onActivityDestroyed(screen.get())
        assertTrue(connector.lastReleased)
    }

    @Test
    fun `a screen that is closed for a rotation keeps the connection`() {
        watcher.onActivityCreated(screen.get(), null)

        watcher.onActivityDestroyed(Robolectric.buildActivity(RotatingScreen::class.java).get())

        assertFalse(connector.lastReleased)
    }
}

/** A screen that the system closes only to build it again. */
class RotatingScreen : Activity() {
    override fun isChangingConfigurations(): Boolean = true
}
