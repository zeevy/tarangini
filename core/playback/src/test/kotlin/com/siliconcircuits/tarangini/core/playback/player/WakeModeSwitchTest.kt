/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WakeModeSwitchTest {
    @Test
    fun `a file and an empty player keep only the processor awake`() {
        assertEquals(C.WAKE_MODE_LOCAL, wakeModeFor(MediaItem.fromUri("content://media/external/audio/media/7")))
        assertEquals(C.WAKE_MODE_LOCAL, wakeModeFor(MediaItem.fromUri("file:///sdcard/Music/a.mp3")))
        assertEquals(C.WAKE_MODE_LOCAL, wakeModeFor(MediaItem.Builder().setMediaId("no address").build()))
        assertEquals(C.WAKE_MODE_LOCAL, wakeModeFor(null))
    }

    @Test
    fun `a stream keeps the network awake too`() {
        assertEquals(C.WAKE_MODE_NETWORK, wakeModeFor(MediaItem.fromUri("https://example.org/live")))
        assertEquals(C.WAKE_MODE_NETWORK, wakeModeFor(MediaItem.fromUri("http://example.org:8000/stream.mp3")))
        assertEquals(C.WAKE_MODE_NETWORK, wakeModeFor(MediaItem.fromUri("HTTPS://example.org/live")))
    }
}
