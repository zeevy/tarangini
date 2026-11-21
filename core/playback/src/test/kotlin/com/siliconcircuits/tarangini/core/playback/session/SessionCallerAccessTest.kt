/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.os.Bundle
import android.os.Looper
import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.siliconcircuits.tarangini.core.playback.TestPlaybackApp
import com.siliconcircuits.tarangini.core.playback.browse.BrowseItems
import com.siliconcircuits.tarangini.core.playback.browse.BrowseLabels
import com.siliconcircuits.tarangini.core.playback.browse.BrowseTree
import com.siliconcircuits.tarangini.core.playback.browse.CarSearch
import com.siliconcircuits.tarangini.core.playback.browse.LibraryList
import com.siliconcircuits.tarangini.core.playback.browse.MediaId
import com.siliconcircuits.tarangini.core.playback.service.PlaybackService
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryLetters
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlayHistory
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSearchRepository
import com.siliconcircuits.tarangini.core.testing.library.testSong
import kotlinx.coroutines.MainScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** What the media panel of the phone and the buttons of the session may do. */
// The test only way to make a caller is an API that Media3 may still change.
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPlaybackApp::class)
class SessionCallerAccessTest {
    private val controller = Robolectric.buildService(PlaybackService::class.java)
    private val service = controller.create().get()
    private val library = FakeLibraryRepository()
    private val callback = run {
        val items = BrowseItems(labels)
        val tree = BrowseTree(
            library,
            FakeLibraryLetters(library),
            items,
            FakeFavouriteRepository(),
            FakePlayHistory(),
            FakePlaylistRepository(),
        )
        val search = CarSearch(FakeSearchRepository(), library, items)
        val car = CarLibrary(
            tree,
            search,
            CarPlay(MainScope(), tree, search, library) { null },
            CarProblems(noAccess = { null }, noSongs = { null }),
        )
        val callers = CallerPolicy(service.packageName, Process.myUid()) { name, uid -> name == CAR && uid == CAR_UID }
        PlaybackSessionCallback(callers, MainScope(), car, { it }, { _, _, _ -> }, { null })
    }
    private val theCar = caller(CAR, CAR_UID, isTrusted = true)
    private val session = requireNotNull(service.onGetSession(caller(service.packageName, Process.myUid(), false)))

    private fun trusted(packageName: String, uidStep: Int) = caller(packageName, Process.myUid() + uidStep, true)

    @After
    fun destroyService() {
        controller.destroy()
    }

    @Test
    fun `the media panel of the phone may ask for the item played last, and gets no list of the library`() {
        library.songs = listOf(testSong(1))
        val systemUi = trusted("com.android.systemui", 2)

        val commands = callback.onConnect(session, systemUi).availableSessionCommands
        val root = callback.onGetLibraryRoot(session, systemUi, null).get()
        val songs = callback.onGetChildren(session, systemUi, MediaId.Section(LibraryList.SONGS).encode(), 0, 100, null)
        shadowOf(Looper.getMainLooper()).idle()

        // Media3 answers the request for the last item itself, and needs these two commands for it.
        assertTrue(commands.contains(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT))
        assertTrue(commands.contains(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN))
        assertEquals(SessionError.ERROR_PERMISSION_DENIED, root.resultCode)
        assertNotEquals(LibraryResult.RESULT_SUCCESS, songs.get().resultCode)
        assertEquals(null, songs.get().value)
    }

    @Test
    fun `another caller the system trusts gets no command of the library`() {
        val headset = trusted("com.android.bluetooth", 3)

        val commands = callback.onConnect(session, headset).availableSessionCommands

        assertFalse(commands.contains(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT))
        assertFalse(commands.contains(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN))
    }

    @Test
    fun `an app that only has the name of the media panel is refused`() {
        val fake = caller("com.android.systemui", Process.myUid() + 4, isTrusted = false)

        assertFalse(callback.onConnect(session, fake).isAccepted)
    }

    @Test
    fun `a press of shuffle or repeat reaches the player`() {
        val player = session.player
        player.setMediaItem(MediaItem.fromUri("content://media/external/audio/media/1"))
        shadowOf(Looper.getMainLooper()).idle()
        callback.buttons = SessionButtons(player, { it.name }) { }

        val shuffle = callback.onCustomCommand(session, theCar, SessionButtons.SHUFFLE, Bundle.EMPTY).get()
        val repeat = callback.onCustomCommand(session, theCar, SessionButtons.REPEAT, Bundle.EMPTY).get()

        assertEquals(SessionResult.RESULT_SUCCESS, shuffle.resultCode)
        assertEquals(SessionResult.RESULT_SUCCESS, repeat.resultCode)
        assertTrue(player.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
        assertTrue(callback.onConnect(session, theCar).availableSessionCommands.contains(SessionButtons.SHUFFLE))
    }
}

private val labels = BrowseLabels(
    "Library", "Songs", "Albums", "Artists", "Folders", "No album", "No artist", null,
    favourites = "Favourites",
    favouritesIcon = null,
    recent = "Recent",
    recentIcon = null,
    playlists = "Playlists",
    mostPlayed = "Most played",
    mostPlayedIcon = null,
)

private fun caller(packageName: String, uid: Int, isTrusted: Boolean): MediaSession.ControllerInfo =
    MediaSession.ControllerInfo.createTestOnlyControllerInfo(
        packageName,
        1,
        uid,
        MediaLibraryInfo.VERSION_INT,
        MediaLibraryInfo.INTERFACE_VERSION,
        isTrusted,
        Bundle.EMPTY,
        true,
    )

private const val CAR = "com.google.android.projection.gearhead"
private val CAR_UID = Process.myUid() + 7
