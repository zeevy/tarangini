/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.service

import android.net.Uri
import android.os.Bundle
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.siliconcircuits.swaratarang.core.playback.session.CallerPolicy
import com.siliconcircuits.swaratarang.core.playback.session.PlaybackSessionCallback
import java.util.concurrent.ExecutionException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

// The test only way to make a caller is an API that Media3 may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackServiceTest {
    private val controller = Robolectric.buildService(PlaybackService::class.java)
    private val service = controller.create().get()
    private val callback = PlaybackSessionCallback(CallerPolicy(service.packageName, Process.myUid()))

    private val thisApp = caller(service.packageName, Process.myUid(), trusted = false)
    private val unknownApp = caller("com.unknown.app", Process.myUid() + 1, trusted = false)
    private val systemUi = caller("com.android.systemui", Process.myUid() + 2, trusted = true)

    @After
    fun destroyService() {
        controller.destroy()
    }

    @Test
    fun `every caller gets the same one session with the same one player`() {
        val first = requireNotNull(service.onGetSession(thisApp))
        val second = requireNotNull(service.onGetSession(systemUi))

        assertSame(first, second)
        assertSame(first.player, second.player)
    }

    @Test
    fun `nothing plays by itself when the service starts`() {
        val player = requireNotNull(service.onGetSession(thisApp)).player

        assertFalse(player.playWhenReady)
        assertEquals(0, player.mediaItemCount)
    }

    @Test
    fun `this app and a caller the system trusts may connect`() {
        val session = requireNotNull(service.onGetSession(thisApp))

        assertTrue(callback.onConnect(session, thisApp).isAccepted)
        assertTrue(callback.onConnect(session, systemUi).isAccepted)
    }

    @Test
    fun `an unknown app that tries to connect is refused`() {
        val session = requireNotNull(service.onGetSession(thisApp))

        assertFalse(callback.onConnect(session, unknownApp).isAccepted)
    }

    @Test
    fun `an app that only claims our package name is refused`() {
        val session = requireNotNull(service.onGetSession(thisApp))
        val impostor = caller(service.packageName, Process.myUid() + 1, trusted = false)

        assertFalse(callback.onConnect(session, impostor).isAccepted)
    }

    @Test
    fun `a MediaStore song a caller asks for is passed to the player`() {
        val session = requireNotNull(service.onGetSession(thisApp))
        val song = MediaItem.fromUri("content://media/external/audio/media/7")

        val accepted = callback.onAddMediaItems(session, thisApp, listOf(song)).get()

        assertEquals(
            listOf(Uri.parse("content://media/external/audio/media/7")),
            accepted.map {
                it.localConfiguration?.uri
            },
        )
    }

    @Test
    fun `a list with one item that is not a MediaStore song is refused as a whole`() {
        val session = requireNotNull(service.onGetSession(thisApp))
        val items = listOf(
            MediaItem.fromUri("content://media/external/audio/media/7"),
            MediaItem.fromUri("file:///data/data/other/secret"),
        )

        val result = callback.onAddMediaItems(session, thisApp, items)

        assertThrows(ExecutionException::class.java) { result.get() }
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
