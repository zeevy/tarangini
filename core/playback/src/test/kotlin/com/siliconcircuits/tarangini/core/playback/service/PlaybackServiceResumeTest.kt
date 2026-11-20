/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.service

import android.os.Bundle
import android.os.Looper
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback
import com.siliconcircuits.tarangini.core.playback.TestPlaybackApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// The test only way to make a caller is API that Media3 may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPlaybackApp::class)
class PlaybackServiceResumeTest {
    private val app = RuntimeEnvironment.getApplication() as TestPlaybackApp
    private val song = "content://media/external/audio/media/"

    @Test
    fun `a new service comes back with the saved queue and place, paused, and saves the place when it ends`() {
        app.resumeStore.saved = SavedPlayback(
            mediaIds = listOf(song + 1, song + 2),
            place = ResumePlace(index = 1, positionMs = 30_000, shuffle = false, repeat = RepeatMode.ONE),
        )
        val controller = Robolectric.buildService(PlaybackService::class.java)
        val service = controller.create().get()
        shadowOf(Looper.getMainLooper()).idle()
        val player = requireNotNull(service.onGetSession(caller())).player

        assertEquals(2, player.mediaItemCount)
        assertEquals(1, player.currentMediaItemIndex)
        assertEquals(30_000L, player.currentPosition)
        assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
        assertFalse(player.playWhenReady)
        assertEquals(Player.STATE_IDLE, player.playbackState)

        player.seekTo(0, 9_000)
        controller.destroy()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(ResumePlace(0, 9_000, shuffle = false, repeat = RepeatMode.ONE), app.resumeStore.saved?.place)
        assertEquals(0, app.resumeStore.queueSaves)
    }

    private fun caller(): MediaSession.ControllerInfo = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
        "com.first.app",
        1,
        Process.myUid() + 1,
        MediaLibraryInfo.VERSION_INT,
        MediaLibraryInfo.INTERFACE_VERSION,
        false,
        Bundle.EMPTY,
        true,
    )
}
