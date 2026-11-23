/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongFormat
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.library.FakeFavouriteRepository
import com.siliconcircuits.tarangini.core.testing.library.FakePlaylistRepository
import com.siliconcircuits.tarangini.core.testing.library.FakeSongLookup
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import com.siliconcircuits.tarangini.core.testing.playback.FakeSleepTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {
    private val playlists = FakePlaylistRepository()

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val queue = FakePlaybackQueue()
    private val song = PlaybackItem("test:1", "Bhairavi", "Ravi", "Morning Ragas", "test:1/art")

    private fun loaded(state: PlaybackState = PlaybackState()) = state.copy(queue = listOf(song), currentIndex = 0)

    private val favourites = FakeFavouriteRepository()
    private val sleep = FakeSleepTimer()
    private var songs = emptyList<Song>()

    private fun TestScope.watched(): NowPlayingViewModel =
        NowPlayingViewModel(playback, queue, favourites, FakeSongLookup(songs), playlists, sleep).also { viewModel ->
            backgroundScope.launch { viewModel.state.collect() }
            runCurrent()
        }

    @Test
    fun `nothing is shown while nothing is loaded`() = runTest(mainDispatcher.dispatcher) {
        assertNull(watched().state.value)
    }

    @Test
    fun `the current item shows with its texts, its place and its length`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()

        playback.state.value = loaded(
            PlaybackState(playWhenReady = true, positionMs = 45_000, durationMs = 180_000, shuffle = true),
        )
        runCurrent()

        assertEquals(
            NowPlayingUiState(
                title = "Bhairavi",
                artist = "Ravi",
                album = "Morning Ragas",
                artworkUri = "test:1/art",
                showPause = true,
                isBuffering = false,
                positionMs = 45_000,
                durationMs = 180_000,
                shuffle = true,
                repeat = RepeatMode.OFF,
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `the list the queue came from is shown`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()
        val album = QueueSource(QueueSourceKind.ALBUM, "Morning Ragas")

        playback.state.value = loaded(PlaybackState(durationMs = 180_000, source = album))
        runCurrent()

        assertEquals(album, viewModel.state.value?.source)
    }

    @Test
    fun `the format and the place in the queue come from the player`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()
        val format = SongFormat("MP3", 320, 44_100, 2)

        playback.state.value = PlaybackState(
            queue = listOf(song, song.copy(id = "test:2"), song.copy(id = "test:3")),
            currentIndex = 1,
            durationMs = 180_000,
            format = format,
        )
        runCurrent()

        assertEquals(format, viewModel.state.value?.format)
        assertEquals(2, viewModel.state.value?.place)
        assertEquals(3, viewModel.state.value?.queueSize)
    }

    @Test
    fun `a stream with no length is live`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()

        playback.state.value = loaded(PlaybackState(playWhenReady = true, isBuffering = true, positionMs = 9_000))
        runCurrent()

        assertEquals(true, viewModel.state.value?.isLive)
        assertEquals(true, viewModel.state.value?.isBuffering)
    }

    @Test
    fun `after a seek the place asked for shows until the player is there`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()
        playback.state.value = loaded(PlaybackState(playWhenReady = true, positionMs = 10_000, durationMs = 180_000))
        runCurrent()

        viewModel.seekTo(120_000)
        runCurrent()
        assertEquals(listOf("seekTo 120000"), playback.commands)
        assertEquals(120_000L, viewModel.state.value?.positionMs)

        // The player still reports the old place for a moment. The screen must not go back to it.
        playback.state.value = playback.state.value.copy(positionMs = 10_400)
        runCurrent()
        assertEquals(120_000L, viewModel.state.value?.positionMs)

        playback.state.value = playback.state.value.copy(positionMs = 120_300)
        runCurrent()
        playback.state.value = playback.state.value.copy(positionMs = 123_000)
        runCurrent()
        assertEquals(123_000L, viewModel.state.value?.positionMs)
    }

    @Test
    fun `a short seek shows the place asked for while the player still reports the old one`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = watched()
            playback.state.value = loaded(PlaybackState(positionMs = 10_000, durationMs = 180_000))
            runCurrent()

            viewModel.seekTo(10_800)
            runCurrent()
            assertEquals(10_800L, viewModel.state.value?.positionMs)

            playback.state.value = playback.state.value.copy(positionMs = 10_900)
            runCurrent()
            assertEquals(10_900L, viewModel.state.value?.positionMs)
        }

    @Test
    fun `the place asked for in one item is not shown for the next one`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()
        val next = PlaybackItem("test:2", "Yaman")
        playback.state.value =
            PlaybackState(queue = listOf(song, next), currentIndex = 0, positionMs = 10_000, durationMs = 180_000)
        runCurrent()

        viewModel.seekTo(170_000)
        playback.state.value = playback.state.value.copy(currentIndex = 1, positionMs = 0)
        runCurrent()

        assertEquals("Yaman", viewModel.state.value?.title)
        assertEquals(0L, viewModel.state.value?.positionMs)
    }

    @Test
    fun `a seek the player does not follow is given up after a short time`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()
        playback.state.value = loaded(PlaybackState(positionMs = 10_000, durationMs = 180_000))
        runCurrent()

        viewModel.seekTo(120_000)
        advanceTimeBy(3_000)

        assertEquals(10_000L, viewModel.state.value?.positionMs)
    }

    @Test
    fun `the place never shows past the end of the item`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()

        playback.state.value = loaded(PlaybackState(positionMs = 200_000, durationMs = 180_000))
        runCurrent()

        assertEquals(180_000L, viewModel.state.value?.positionMs)
    }

    @Test
    fun `the play button offers play after an error, and each button sends its command`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = watched()
            playback.state.value = loaded(PlaybackState(playWhenReady = true, error = PlaybackError.CANNOT_PLAY))
            runCurrent()
            assertEquals(false, viewModel.state.value?.showPause)
            assertEquals(PlaybackError.CANNOT_PLAY, viewModel.state.value?.error)

            viewModel.togglePlay()
            viewModel.next()
            viewModel.previous()

            assertEquals(listOf("resume", "next", "previous"), playback.commands)
        }

    @Test
    fun `shuffle turns on and off, and repeat goes from off to all to one and back`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = watched()

            viewModel.toggleShuffle()
            playback.state.value = loaded(PlaybackState(shuffle = true))
            viewModel.toggleShuffle()
            viewModel.cycleRepeat()
            playback.state.value = loaded(PlaybackState(repeat = RepeatMode.ALL))
            viewModel.cycleRepeat()
            playback.state.value = loaded(PlaybackState(repeat = RepeatMode.ONE))
            viewModel.cycleRepeat()

            assertEquals(
                listOf("setShuffle true", "setShuffle false", "setRepeat ALL", "setRepeat ONE", "setRepeat OFF"),
                queue.commands,
            )
        }

    @Test
    fun `the heart shows for a song of the library and a press changes its mark`() =
        runTest(mainDispatcher.dispatcher) {
            songs = listOf(testSong(1))
            val viewModel = watched()
            playback.state.value = loaded()
            runCurrent()
            val before = viewModel.state.value?.isFavourite

            viewModel.toggleFavourite()
            runCurrent()

            assertEquals(false, before)
            assertEquals(true, viewModel.state.value?.isFavourite)
            assertEquals(listOf(1L), favourites.ids.value)
        }

    @Test
    fun `an item that is no song of the library has no heart`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()
        playback.state.value = loaded()
        runCurrent()

        viewModel.toggleFavourite()
        runCurrent()

        assertNull(viewModel.state.value?.isFavourite)
        assertEquals(emptyList<Long>(), favourites.ids.value)
    }

    @Test
    fun `the song that plays can be put into a new playlist`() = runTest(mainDispatcher.dispatcher) {
        songs = listOf(testSong(1))
        playlists.known = songs
        val viewModel = watched()
        playback.state.value = loaded()
        runCurrent()

        viewModel.playlistPick.onCreate(requireNotNull(viewModel.state.value?.songId), "Drive")
        runCurrent()

        assertEquals(listOf("Drive" to 1), playlists.playlists().map { it.name to it.songCount })
    }

    @Test
    fun `the speed and the sleep timer show, and a pick reaches the player and the timer`() =
        runTest(mainDispatcher.dispatcher) {
            playback.state.value = loaded(PlaybackState(speed = 1.5f))
            val viewModel = watched()

            viewModel.options.onSetSpeed(2f)
            viewModel.options.onSleepAfter(30)
            runCurrent()
            val timed = viewModel.state.value
            viewModel.options.onSleepAtEnd()
            runCurrent()
            val atEnd = viewModel.state.value?.sleep
            viewModel.options.onCancelSleep()
            runCurrent()

            assertEquals(1.5f to SleepState.Timed(1_800_000), timed?.speed to timed?.sleep)
            assertEquals(SleepState.EndOfTrack, atEnd)
            assertEquals(SleepState.Off, viewModel.state.value?.sleep)
            assertEquals(listOf("setSpeed 2.0"), playback.commands)
        }
}
