/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.miniplayer

import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MiniPlayerViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val song = PlaybackItem(id = "test:1", title = "Bhairavi", artist = "Ravi", artworkUri = "test:1/art")

    private fun loaded(state: PlaybackState = PlaybackState()) = state.copy(queue = listOf(song), currentIndex = 0)

    @Test
    fun `nothing is shown while nothing is loaded`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = MiniPlayerViewModel(playback)
        backgroundScope.launch { viewModel.state.collect() }

        assertNull(viewModel.state.value)
    }

    @Test
    fun `the current item shows with its texts, picture and progress`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = MiniPlayerViewModel(playback)
        backgroundScope.launch { viewModel.state.collect() }

        playback.state.value =
            loaded(PlaybackState(isPlaying = true, playWhenReady = true, positionMs = 45_000, durationMs = 180_000))

        assertEquals(
            MiniPlayerUiState(
                "Bhairavi",
                "Ravi",
                "test:1/art",
                showPause = true,
                isBuffering = false,
                progress = 0.25f,
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `an item that is already loaded shows from the start`() = runTest(mainDispatcher.dispatcher) {
        playback.state.value = loaded()

        val viewModel = MiniPlayerViewModel(playback)

        assertEquals("Bhairavi", viewModel.state.value?.title)
    }

    @Test
    fun `a stream with no known length has no progress, and says when it waits for data`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = MiniPlayerViewModel(playback)
            backgroundScope.launch { viewModel.state.collect() }

            playback.state.value = loaded(PlaybackState(playWhenReady = true, isBuffering = true, positionMs = 9_000))

            assertNull(viewModel.state.value?.progress)
            assertEquals(true, viewModel.state.value?.isBuffering)
            assertEquals(true, viewModel.state.value?.showPause)
        }

    @Test
    fun `after an error and at the end of the queue the button offers play, and a tap goes on`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = MiniPlayerViewModel(playback)
            backgroundScope.launch { viewModel.state.collect() }

            playback.state.value = loaded(PlaybackState(playWhenReady = true, error = PlaybackError.CANNOT_PLAY))
            assertEquals(false, viewModel.state.value?.showPause)
            viewModel.togglePlay()

            playback.state.value = loaded(PlaybackState(playWhenReady = true, isEnded = true))
            assertEquals(false, viewModel.state.value?.showPause)
            viewModel.togglePlay()

            assertEquals(listOf("resume", "resume"), playback.commands)
        }

    @Test
    fun `a paused stream that fills its buffer does not show as loading`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = MiniPlayerViewModel(playback)
        backgroundScope.launch { viewModel.state.collect() }

        playback.state.value = loaded(PlaybackState(playWhenReady = false, isBuffering = true))

        assertEquals(false, viewModel.state.value?.isBuffering)
    }

    @Test
    fun `the progress stays between nothing and all`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = MiniPlayerViewModel(playback)
        backgroundScope.launch { viewModel.state.collect() }

        playback.state.value = loaded(PlaybackState(positionMs = 200_000, durationMs = 180_000))
        assertEquals(1f, viewModel.state.value?.progress)

        playback.state.value = loaded(PlaybackState(positionMs = 5, durationMs = 0))
        assertNull(viewModel.state.value?.progress)
    }

    @Test
    fun `the button pauses when play was asked for, and goes on when not`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = MiniPlayerViewModel(playback)
        backgroundScope.launch { viewModel.state.collect() }

        playback.state.value = loaded(PlaybackState(playWhenReady = true, isBuffering = true))
        viewModel.togglePlay()
        playback.state.value = loaded(PlaybackState(playWhenReady = false))
        viewModel.togglePlay()
        viewModel.next()

        assertEquals(listOf("pause", "resume", "next"), playback.commands)
    }
}
