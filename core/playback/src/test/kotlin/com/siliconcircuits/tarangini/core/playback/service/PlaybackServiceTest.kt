/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.service

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.siliconcircuits.tarangini.core.playback.TestPlaybackApp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// The test only way to make a caller, and the "is released" flag of the player, are API that Media3 may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPlaybackApp::class)
class PlaybackServiceTest {
    private val app: Application = RuntimeEnvironment.getApplication().also(::addLauncherScreen)
    private val controller = Robolectric.buildService(PlaybackService::class.java)
    private val service = controller.create().get()

    private val firstCaller = caller("com.first.app", Process.myUid() + 1)
    private val secondCaller = caller("com.second.app", Process.myUid() + 2)

    private var ended = false

    @After
    fun endService() {
        if (!ended) controller.destroy()
    }

    @Test
    fun `every caller gets the same one session with the same one player`() {
        val first = requireNotNull(service.onGetSession(firstCaller))
        val second = requireNotNull(service.onGetSession(secondCaller))

        assertSame(first, second)
        assertSame(first.player, second.player)
    }

    @Test
    fun `nothing plays by itself when the service starts`() {
        val player = requireNotNull(service.onGetSession(firstCaller)).player

        assertFalse(player.playWhenReady)
        assertEquals(0, player.mediaItemCount)
    }

    @Test
    fun `a tap on the notification opens this app`() {
        val tap = requireNotNull(service.onGetSession(firstCaller)).sessionActivity

        assertEquals(app.packageName, shadowOf(tap).savedIntent.component?.packageName)
        assertTrue(shadowOf(tap).isImmutable)
    }

    @Test
    fun `the service stops itself when the app is swiped away and nothing plays`() {
        service.onTaskRemoved(null)

        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun `the player and the session are released when the service ends`() {
        val player = requireNotNull(service.onGetSession(firstCaller)).player as ExoPlayer

        controller.destroy()
        ended = true

        assertTrue(player.isReleased)
        assertNull(service.onGetSession(firstCaller))
    }

    /** A library module has no screen of its own, so the test gives the package the launcher screen the app has. */
    private fun addLauncherScreen(app: Application) {
        val screen = ComponentName(app.packageName, "${app.packageName}.MainActivity")
        val launcher = IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        shadowOf(app.packageManager).addActivityIfNotPresent(screen)
        shadowOf(app.packageManager).addIntentFilterForActivity(screen, launcher)
    }

    private fun caller(packageName: String, uid: Int): MediaSession.ControllerInfo =
        MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            packageName,
            1,
            uid,
            MediaLibraryInfo.VERSION_INT,
            MediaLibraryInfo.INTERFACE_VERSION,
            false,
            Bundle.EMPTY,
            true,
        )
}
