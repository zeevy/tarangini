/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ResetSpeedTest {
    private val player = ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build()
        .also { it.addListener(ResetSpeed(it)) }

    private fun item(id: Int) = MediaItem.fromUri("content://media/external/audio/media/$id")

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    @After
    fun release() = player.release()

    @Test
    fun `a new queue starts at the normal speed`() {
        player.setMediaItems(listOf(item(1), item(2)))
        settle()
        player.setPlaybackSpeed(1.5f)
        settle()

        player.setMediaItems(listOf(item(3)))
        settle()

        assertEquals(1f, player.playbackParameters.speed)
    }

    @Test
    fun `a song added to the queue leaves the speed as it is`() {
        player.setMediaItems(listOf(item(1)))
        settle()
        player.setPlaybackSpeed(1.5f)
        settle()

        player.addMediaItem(item(2))
        settle()

        assertEquals(1.5f, player.playbackParameters.speed)
    }
}
