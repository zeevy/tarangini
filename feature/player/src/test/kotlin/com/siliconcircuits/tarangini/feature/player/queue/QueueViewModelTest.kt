/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.PlaybackState
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackQueue
import com.siliconcircuits.tarangini.feature.player.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueueViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val playback = FakePlaybackController()
    private val queue = FakePlaybackQueue()
    private val songs = listOf(PlaybackItem("a", "First"), PlaybackItem("b", "Second"), PlaybackItem("c", "Third"))

    @Test
    fun `the queue shows with the place of the item that plays, also when it was loaded before`() =
        runTest(mainDispatcher.dispatcher) {
            playback.state.value = PlaybackState(queue = songs, currentIndex = 1)

            val viewModel = QueueViewModel(playback, queue)

            assertEquals(QueueUiState(songs, 1), viewModel.state.value)
        }

    @Test
    fun `a new place of the player does not give a new list, a new current item does`() =
        runTest(mainDispatcher.dispatcher) {
            playback.state.value = PlaybackState(queue = songs, currentIndex = 1)
            val viewModel = QueueViewModel(playback, queue)
            val seen = mutableListOf<QueueUiState>()
            backgroundScope.launch { viewModel.state.collect { seen += it } }
            runCurrent()

            playback.state.value = playback.state.value.copy(positionMs = 5_000)
            runCurrent()
            playback.state.value = playback.state.value.copy(currentIndex = 2)
            runCurrent()

            assertEquals(listOf(1, 2), seen.map { it.currentIndex })
            assertSame(songs, seen.last().items)
        }

    @Test
    fun `play, move and remove reach the queue with their places`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = QueueViewModel(playback, queue)

        viewModel.playAt(2)
        viewModel.move(from = 2, to = 0)
        viewModel.remove(1)

        assertEquals(listOf("playAt 2", "move 2 to 0", "remove 1"), queue.commands)
    }
}
