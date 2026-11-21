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
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.playback.TestPlaybackApp
import com.siliconcircuits.tarangini.core.playback.browse.BrowseItems
import com.siliconcircuits.tarangini.core.playback.browse.BrowseLabels
import com.siliconcircuits.tarangini.core.playback.browse.BrowseTree
import com.siliconcircuits.tarangini.core.playback.browse.CarSearch
import com.siliconcircuits.tarangini.core.playback.browse.LibraryList
import com.siliconcircuits.tarangini.core.playback.browse.MediaId
import com.siliconcircuits.tarangini.core.playback.mapper.LibraryTexts
import com.siliconcircuits.tarangini.core.playback.service.PlaybackService
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryLetters
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlayHistory
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSearchRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSongLookup
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.station.FakeStationStreams
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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
    private val favourites = FakeFavouriteRepository()
    private val history = FakePlayHistory()
    private val controller = Robolectric.buildService(PlaybackService::class.java)
    private val service = controller.create().get()
    private var saved: MediaItemsWithStartPosition? = null
    private var readFailure: Exception? = null
    private var texts: suspend (List<MediaItem>) -> List<MediaItem> = { it }
    private val car = KnownCallers { packageName, uid -> packageName == CAR && uid == Process.myUid() + CAR_UID_STEP }
    private val library = FakeLibraryRepository()
    private val search = FakeSearchRepository()
    private var allowed = true
    private val callback = callbackOn(library, { texts(it) }) {
        readFailure?.let { throw it }
            ?: saved
    }

    private fun callbackOn(
        library: LibraryRepository,
        texts: suspend (List<MediaItem>) -> List<MediaItem>,
        resumption: suspend () -> MediaItemsWithStartPosition?,
    ): PlaybackSessionCallback {
        val items = BrowseItems(labels)
        val tree = BrowseTree(library, FakeLibraryLetters(this.library), items, favourites, history, playlists)
        val carSearch = CarSearch(search, library, items)
        val carPlay = CarPlay(MainScope(), tree, carSearch, library, resumption)
        val problems = CarProblems(
            noAccess = {
                SessionError(SessionError.ERROR_SESSION_AUTHENTICATION_EXPIRED, "Allow").takeUnless { allowed }
            },
            noSongs = { SessionError(SessionError.INFO_CANCELLED, "No songs").takeIf { this.library.songs.isEmpty() } },
        )
        val callers = CallerPolicy(service.packageName, Process.myUid(), car)
        val car = CarLibrary(tree, carSearch, carPlay, problems)
        return PlaybackSessionCallback(callers, MainScope(), car, texts, { _, _, _ -> }, resumption)
    }
    private val theCar = caller(CAR, Process.myUid() + CAR_UID_STEP, trusted = true)

    private val thisApp = caller(service.packageName, Process.myUid(), trusted = false)
    private val session = requireNotNull(service.onGetSession(thisApp))

    /** The item as the session takes it. The read of the texts runs on the main thread, so that runs first. */
    private fun added(item: MediaItem): MediaItem {
        val answer = callback.onAddMediaItems(session, thisApp, listOf(item))
        shadowOf(Looper.getMainLooper()).idle()
        return answer.get().single()
    }

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
        // It cannot read or search the library either: the lists are for this app and the car.
        listOf(
            SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM,
            SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE,
            SessionCommand.COMMAND_CODE_LIBRARY_SEARCH,
        ).forEach { command -> assertFalse(result.availableSessionCommands.contains(command)) }
    }

    @Test
    fun `the car may browse the library and play from it`() {
        val result = callback.onConnect(session, theCar)

        assertTrue(result.isAccepted)
        assertTrue(result.availableSessionCommands.contains(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN))
        assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_SET_MEDIA_ITEM))
    }

    @Test
    fun `the car gets the top of the library, and the Library tab with its lists under it`() {
        library.songs = listOf(testSong(1))
        val root = callback.onGetLibraryRoot(session, theCar, null).get()
        val tabs = children(MediaId.Root.encode())
        val lists = children(tabs.first().mediaId)

        assertEquals(MediaId.Root.encode(), root.value?.mediaId)
        assertEquals(true, root.value?.mediaMetadata?.isBrowsable)
        assertEquals(listOf("Library", "Favourites", "Recent", "Most played"), tabs.map { it.mediaMetadata.title })
        assertEquals(listOf("Songs", "Albums", "Artists", "Folders", "Playlists"), lists.map { it.mediaMetadata.title })
    }

    @Test
    fun `a car that shows fewer tabs gets fewer, and a car that asks for none still gets one`() {
        val none = LibraryParams.Builder()
            .setExtras(Bundle().apply { putInt(MediaConstants.EXTRAS_KEY_ROOT_CHILDREN_LIMIT, 0) })
            .build()

        callback.onGetLibraryRoot(session, theCar, none).get()

        assertEquals(1, children(MediaId.Root.encode()).size)
    }

    @Test
    fun `an id this app did not make gives an error and no crash`() {
        listOf("", "radio", "album.x", "song.1@root", "../..", "a".repeat(10_000)).forEach { badId ->
            val list = callback.onGetChildren(session, theCar, badId, 0, 100, null).get()
            val item = callback.onGetItem(session, theCar, badId).get()

            assertEquals(SessionError.ERROR_BAD_VALUE, list.resultCode)
            assertEquals(SessionError.ERROR_BAD_VALUE, item.resultCode)
        }
        // A song has nothing under it, and a song the library does not have is not an item.
        library.songs = listOf(testSong(1))
        val song = MediaId.SongIn(1, MediaId.Album(1)).encode()
        assertEquals(
            SessionError.ERROR_BAD_VALUE,
            callback.onGetChildren(session, theCar, song, 0, 100, null).get().resultCode,
        )
        val gone = MediaId.SongIn(404, MediaId.Album(1)).encode()
        assertEquals(SessionError.ERROR_BAD_VALUE, callback.onGetItem(session, theCar, gone).get().resultCode)
    }

    @Test
    fun `the car gets an error and no crash when the library cannot be read`() {
        val broken = callbackOn(BrokenLibrary(library), { it }) { null }

        val list = broken.onGetChildren(session, theCar, MediaId.Folders.encode(), 0, 100, null).get()

        assertEquals(SessionError.ERROR_UNKNOWN, list.resultCode)
    }

    @Test
    fun `the car gets the whole list at once, and a caller that asks for a page gets that page`() {
        library.songs = (1L..30L).map { testSong(it, title = "Song %02d".format(it)) }
        val songs = MediaId.Section(LibraryList.SONGS).encode()

        fun page(page: Int, size: Int) =
            requireNotNull(callback.onGetChildren(session, theCar, songs, page, size, null).get().value)

        // The car asks with the largest page size there is.
        assertEquals(30, page(0, Int.MAX_VALUE).size)
        assertEquals(
            listOf("Song 16", "Song 17", "Song 18", "Song 19", "Song 20"),
            page(3, 5).map {
                it.mediaMetadata.title
            },
        )
        // Media3 refuses an answer with more rows than the page, so no page is ever longer than asked.
        assertEquals(7, page(0, 7).size)
        assertTrue(page(9, 5).isEmpty())
        assertTrue(page(Int.MAX_VALUE, Int.MAX_VALUE).isEmpty())
        assertTrue(page(1, Int.MAX_VALUE).isEmpty())
    }

    @Test
    fun `a tap on a song in the car plays its list from that song on`() {
        library.songs = listOf(testSong(1, albumId = 7), testSong(2, albumId = 7), testSong(3, albumId = 8))
        val tapped = children(MediaId.Album(7).encode())[1]

        val queue = callback.onSetMediaItems(session, theCar, listOf(tapped), 0, 0).get()

        assertEquals(listOf("test:1", "test:2"), queue.mediaItems.map { it.localConfiguration?.uri.toString() })
        assertEquals(listOf("test:1", "test:2"), queue.mediaItems.map { it.mediaId })
        assertEquals(1, queue.startIndex)
        assertEquals("Song 2", queue.mediaItems[1].mediaMetadata.title)
    }

    @Test
    fun `a tap on a song the library no longer has plays nothing`() {
        val gone = MediaItem.Builder().setMediaId(MediaId.SongIn(404, MediaId.Album(7)).encode()).build()

        val answer = callback.onSetMediaItems(session, theCar, listOf(gone), 0, 0)

        assertTrue(answer.isDone)
        assertThrows(ExecutionException::class.java) { answer.get() }
    }

    @Test
    fun `a song is not added to the queue by its car id, only by its address`() {
        library.songs = listOf(testSong(1, albumId = 7))
        val picked = MediaItem.Builder().setMediaId(MediaId.SongIn(1, MediaId.Album(7)).encode()).build()
        val file = MediaItem.fromUri("content://media/external/audio/media/7")

        val added = callback.onAddMediaItems(session, theCar, listOf(picked, file))

        assertThrows(ExecutionException::class.java) { added.get() }
    }

    @Test
    fun `an item with an address is checked as an address, whatever its id says`() {
        library.songs = listOf(testSong(1, albumId = 7))
        val sly = MediaItem.Builder()
            .setMediaId(MediaId.SongIn(1, MediaId.Album(7)).encode())
            .setUri("file:///data/data/other.app/secret.mp3")
            .build()

        val set = callback.onSetMediaItems(session, theCar, listOf(sly), 0, 0)
        val added = callback.onAddMediaItems(session, theCar, listOf(sly))

        assertThrows(ExecutionException::class.java) { set.get() }
        assertThrows(ExecutionException::class.java) { added.get() }
    }

    @Test
    fun `the lists a caller has read are remembered, the newest last, and not without end`() {
        children(MediaId.Root.encode())
        children(MediaId.Library.encode())
        children(MediaId.Root.encode())

        assertEquals(listOf(MediaId.Library.encode(), MediaId.Root.encode()), callback.opened.all())

        (1L..200L).forEach { children(MediaId.Album(it).encode()) }
        assertEquals(MAX_OPENED, callback.opened.all().size)
        assertEquals(MediaId.Album(200).encode(), callback.opened.all().last())
    }

    /** What the car gets under a node. The car asks with the largest page size there is. */
    private fun children(parentId: String): List<MediaItem> =
        requireNotNull(callback.onGetChildren(session, theCar, parentId, 0, Int.MAX_VALUE, null).get().value)

    @Test
    fun `an app that only claims the package name of the car is refused`() {
        val impostor = caller(CAR, Process.myUid() + 1, trusted = false)

        assertFalse(callback.onConnect(session, impostor).isAccepted)
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
    fun `an accepted item gets the texts of the library, and keeps its checked address`() {
        val uri = "content://media/external/audio/media/7"
        val songs = FakeSongLookup(listOf(testSong(7, title = "Bhairavi", artist = "Ravi").copy(mediaUri = uri)))
        texts = LibraryTexts(songs, FakeStationStreams())::addTo
        val asked = MediaItem.Builder()
            .setMediaId(uri)
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle("A title the caller sent").build())
            .build()

        val accepted = added(asked)

        assertEquals("Bhairavi", accepted.mediaMetadata.title)
        assertEquals("Ravi", accepted.mediaMetadata.artist)
        assertEquals(Uri.parse(uri), accepted.localConfiguration?.uri)
    }

    @Test
    fun `the answer waits for the texts, and comes when they are read`() {
        val read = CompletableDeferred<Unit>()
        texts = { items ->
            read.await()
            items.map { it.buildUpon().setMediaMetadata(MediaMetadata.Builder().setTitle("Read").build()).build() }
        }
        val uri = "content://media/external/audio/media/7"

        val answer = callback.onAddMediaItems(
            session,
            thisApp,
            listOf(MediaItem.Builder().setMediaId(uri).setUri(uri).build()),
        )
        assertFalse(answer.isDone)
        read.complete(Unit)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("Read", answer.get().single().mediaMetadata.title)
    }

    @Test
    fun `an item whose id is not its address keeps the id and gets the texts of the checked address`() {
        val uri = "content://media/external/audio/media/7"
        texts =
            LibraryTexts(
                FakeSongLookup(listOf(testSong(7, title = "Bhairavi").copy(mediaUri = uri))),
                FakeStationStreams(),
            )::addTo

        val accepted = added(MediaItem.Builder().setMediaId("another id").setUri(uri).build())

        assertEquals("another id", accepted.mediaId)
        assertEquals(Uri.parse(uri), accepted.localConfiguration?.uri)
        assertEquals("Bhairavi", accepted.mediaMetadata.title)
    }

    @Test
    fun `a stream gets the name its station was stored with, and a stream of no station gets none`() {
        val stored = "https://radio.example/stream"
        texts = LibraryTexts(FakeSongLookup(), FakeStationStreams(names = mapOf(stored to "Radio Bhairavi")))::addTo

        val known = added(MediaItem.Builder().setMediaId(stored).setUri(stored).build())
        val unknown = added(MediaItem.Builder().setMediaId("x").setUri("https://other.example/").build())

        assertEquals("Radio Bhairavi", known.mediaMetadata.station.toString())
        assertEquals("Radio Bhairavi", known.mediaMetadata.title.toString())
        assertEquals(Uri.parse(stored), known.localConfiguration?.uri)
        assertEquals(null, unknown.mediaMetadata.station)
    }

    @Test
    fun `an item is accepted without texts when the library cannot be read`() {
        texts =
            LibraryTexts(
                FakeSongLookup(failure = IllegalStateException("The database is closed")),
                FakeStationStreams(),
            )::addTo
        val uri = "content://media/external/audio/media/7"
        val asked = MediaItem.Builder().setMediaId(uri).setUri(uri).build()

        val accepted = added(asked)

        assertEquals(Uri.parse(uri), accepted.localConfiguration?.uri)
        assertEquals(null, accepted.mediaMetadata.title)
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

    @Test
    fun `the car can search, and asks for the results after it is told there are some`() {
        search.results = SearchResults(songs = listOf(testSong(1, "Bhairavi", albumId = 7)))

        val accepted = callback.onSearch(session, theCar, "bhai", null).get()
        val results = callback.onGetSearchResult(session, theCar, "bhai", 0, 100, null).get()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(LibraryResult.RESULT_SUCCESS, accepted.resultCode)
        assertEquals(listOf(MediaId.SongIn(1, MediaId.Album(7)).encode()), results.value?.map { it.mediaId })
    }

    @Test
    fun `a spoken request from the car plays what it finds, and cannot be added to the queue`() {
        search.results = SearchResults(songs = listOf(testSong(2, "Bhairavi")))
        val request = MediaItem.RequestMetadata.Builder().setSearchQuery("bhairavi").build()
        val spoken = MediaItem.Builder().setRequestMetadata(request).build()

        val queue = callback.onSetMediaItems(session, theCar, listOf(spoken), 0, 0).get()
        val added = callback.onAddMediaItems(session, theCar, listOf(spoken))

        assertEquals(listOf(testSong(2).mediaUri), queue.mediaItems.map { it.mediaId })
        assertThrows(ExecutionException::class.java) { added.get() }
    }

    @Test
    fun `without the audio permission every list of the car is an error that says what to do`() {
        library.songs = listOf(testSong(1))
        allowed = false

        val tabs = callback.onGetChildren(session, theCar, MediaId.Root.encode(), 0, 100, null).get()
        val songs = callback.onGetChildren(
            session,
            theCar,
            MediaId.Section(LibraryList.SONGS).encode(),
            0,
            100,
            null,
        ).get()

        val found = callback.onGetSearchResult(session, theCar, "song", 0, 100, null).get()

        assertEquals(SessionError.ERROR_SESSION_AUTHENTICATION_EXPIRED, tabs.resultCode)
        assertEquals(SessionError.ERROR_SESSION_AUTHENTICATION_EXPIRED, found.resultCode)
        assertEquals("Allow", songs.sessionError?.message)
        // The car is told when the library changes, so the lists come back after the permission is given.
        assertEquals(
            listOf(MediaId.Root.encode(), MediaId.Section(LibraryList.SONGS).encode()),
            callback.opened.all().toList(),
        )
    }

    @Test
    fun `the lists come back when the audio permission is given`() {
        library.songs = listOf(testSong(1))
        allowed = false
        callback.onGetChildren(session, theCar, MediaId.Library.encode(), 0, 100, null).get()
        allowed = true

        val sections = callback.onGetChildren(session, theCar, MediaId.Library.encode(), 0, 100, null).get()

        assertEquals(LibraryResult.RESULT_SUCCESS, sections.resultCode)
        assertEquals(5, sections.value?.size)
    }

    @Test
    fun `an empty library gives an empty Library tab and no error`() {
        library.songs = emptyList()

        val sections = callback.onGetChildren(session, theCar, MediaId.Library.encode(), 0, 100, null).get()
        val tabs = callback.onGetChildren(session, theCar, MediaId.Root.encode(), 0, 100, null).get()

        assertEquals(LibraryResult.RESULT_SUCCESS, sections.resultCode)
        assertEquals(0, sections.value?.size)
        assertEquals(4, tabs.value?.size)
    }

    @Test
    fun `the car and a caller the system trusts may press the heart button, and the press marks the song that plays`() =
        runTest {
            val systemPanel = caller("com.android.systemui", Process.myUid() + 1, trusted = true)
            favourites.known = listOf(testSong(1))
            val button = FavouriteButton(backgroundScope, favourites, FakeSongLookup(favourites.known)) { "Heart" }
            callback.favourite = button
            button.start { }
            button.nowPlaying(testSong(1).mediaUri)
            testScheduler.runCurrent()

            val pressed = callback.onCustomCommand(session, theCar, FavouriteButton.COMMAND, Bundle.EMPTY).get()
            testScheduler.runCurrent()

            assertTrue(callback.onConnect(session, theCar).availableSessionCommands.contains(FavouriteButton.COMMAND))
            assertTrue(
                callback.onConnect(session, systemPanel).availableSessionCommands.contains(FavouriteButton.COMMAND),
            )
            assertEquals(SessionResult.RESULT_SUCCESS, pressed.resultCode)
            assertEquals(listOf(1L), favourites.ids.value)
        }
}

private val playlists = FakePlaylistRepository()

private val labels =
    BrowseLabels(
        "Library", "Songs", "Albums", "Artists", "Folders", "No album", "No artist", null,
        favourites = "Favourites",
        favouritesIcon = null,
        recent = "Recent",
        recentIcon = null,
        playlists = "Playlists",
        mostPlayed = "Most played",
        mostPlayedIcon = null,
    )

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

private const val CAR = "com.google.android.projection.gearhead"
private const val CAR_UID_STEP = 7
private const val MAX_OPENED = 50

/** A library whose folders cannot be read, as when the database fails. */
private class BrokenLibrary(library: LibraryRepository) : LibraryRepository by library {
    override suspend fun folders(parentId: Long?): List<Folder> = error("The database is closed")
}
