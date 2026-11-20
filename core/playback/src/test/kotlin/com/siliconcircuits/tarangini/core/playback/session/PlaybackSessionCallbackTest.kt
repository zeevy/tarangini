/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.siliconcircuits.tarangini.core.playback.TestPlaybackApp
import com.siliconcircuits.tarangini.core.playback.service.PlaybackService
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.MainScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// The test only way to make a caller is an API that Media3 may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPlaybackApp::class)
class PlaybackSessionCallbackTest {
    private val controller = Robolectric.buildService(PlaybackService::class.java)
    private val service = controller.create().get()
    private var saved: MediaItemsWithStartPosition? = null
    private var readFailure: Exception? = null
    private val callback =
        PlaybackSessionCallback(CallerPolicy(service.packageName, Process.myUid()), MainScope()) {
            readFailure?.let { throw it }
                ?: saved
        }

    private val thisApp = caller(service.packageName, Process.myUid(), trusted = false)
    private val session = requireNotNull(service.onGetSession(thisApp))

    @After
    fun destroyService() {
        controller.destroy()
    }

    @Test
    fun `this app may connect and may change the queue`() {
        val result = callback.onConnect(session, thisApp)

        assertTrue(result.isAccepted)
        assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
    }

    @Test
    fun `a caller the system trusts may play, pause, seek and skip, and cannot change the queue`() {
        val systemUi = caller("com.android.systemui", Process.myUid() + 2, trusted = true)

        val result = callback.onConnect(session, systemUi)

        assertTrue(result.isAccepted)
        listOf(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_PREVIOUS,
        ).forEach { command -> assertTrue(result.availablePlayerCommands.contains(command)) }
        assertFalse(result.availablePlayerCommands.contains(Player.COMMAND_SET_MEDIA_ITEM))
        assertFalse(result.availablePlayerCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS))
    }

    @Test
    fun `an unknown app that tries to connect is refused`() {
        val unknownApp = caller("com.unknown.app", Process.myUid() + 1, trusted = false)

        assertFalse(callback.onConnect(session, unknownApp).isAccepted)
    }

    @Test
    fun `an app that only claims our package name is refused`() {
        val impostor = caller(service.packageName, Process.myUid() + 1, trusted = false)

        assertFalse(callback.onConnect(session, impostor).isAccepted)
    }

    @Test
    fun `a MediaStore song a caller asks for is passed to the player`() {
        val song = MediaItem.fromUri("content://media/external/audio/media/7")

        val accepted = callback.onAddMediaItems(session, thisApp, listOf(song)).get()

        assertEquals(
            listOf(Uri.parse("content://media/external/audio/media/7")),
            accepted.map { it.localConfiguration?.uri },
        )
    }

    @Test
    fun `a list with one item that is not a MediaStore song is refused as a whole`() {
        val items = listOf(
            MediaItem.fromUri("content://media/external/audio/media/7"),
            MediaItem.fromUri("file:///data/data/other/secret"),
        )

        val result = callback.onAddMediaItems(session, thisApp, items)

        assertThrows(ExecutionException::class.java) { result.get() }
    }

    @Test
    fun `a request to go on with an empty player gets what was saved`() {
        val song = MediaItem.Builder().setMediaId("song").build()
        saved = MediaItemsWithStartPosition(listOf(song), 0, 12_000)

        val answer = callback.onPlaybackResumption(session, thisApp, true)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(saved, answer.get())
    }

    @Test
    fun `a request to go on fails when nothing was saved`() {
        val answer = callback.onPlaybackResumption(session, thisApp, true)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(runCatching { answer.get() }.exceptionOrNull()?.cause is UnsupportedOperationException)
    }

    @Test
    fun `a request to go on gets an answer when the read of the saved queue fails`() {
        readFailure = IllegalStateException("read failed")

        val answer = callback.onPlaybackResumption(session, thisApp, true)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(answer.isDone)
        assertTrue(runCatching { answer.get() }.isFailure)
    }

    private fun caller(packageName: String, uid: Int, trusted: Boolean): MediaSession.ControllerInfo =
        MediaSession.ControllerInfo.createTestOnlyControllerInfo(
            packageName,
            1,
            uid,
            MediaLibraryInfo.VERSION_INT,
            MediaLibraryInfo.INTERFACE_VERSION,
            trusted,
            Bundle.EMPTY,
            true,
        )
}
