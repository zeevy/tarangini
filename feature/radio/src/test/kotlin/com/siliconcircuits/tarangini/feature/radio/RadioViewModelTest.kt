/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio

import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.testing.playback.FakePlaybackController
import com.siliconcircuits.tarangini.core.testing.station.FakeStationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RadioViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val alpha = Station(1, "Alpha", listOf("http://alpha.example/"))
    private val beta = Station(2, "Beta", listOf("https://beta.example/live.m3u8", "https://beta.example/2"))
    private val stations = FakeStationRepository(listOf(beta, alpha))
    private val playback = FakePlaybackController()

    private fun viewModel() = RadioViewModel(stations, playback)

    @Test
    fun `the stations the person added are listed by name`() = runTest(mainDispatcher.dispatcher) {
        assertEquals(RadioUiState(listOf(alpha, beta)), viewModel().state.value)
    }

    @Test
    fun `the list is read again after a station was saved or removed`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        stations.save(null, "Gamma", "http://gamma.example/")
        assertEquals(listOf("Alpha", "Beta", "Gamma"), viewModel.state.value.stations?.map { it.name })

        viewModel.remove(alpha)
        assertEquals(listOf("Beta", "Gamma"), viewModel.state.value.stations?.map { it.name })
    }

    @Test
    fun `a tap plays the station from its first address, with the other stations as the queue`() =
        runTest(mainDispatcher.dispatcher) {
            viewModel().play(beta)

            assertEquals(
                listOf("play [http://alpha.example/, https://beta.example/live.m3u8] from 1"),
                playback.commands,
            )
        }

    @Test
    fun `a station that is not in the list any more is not played`() = runTest(mainDispatcher.dispatcher) {
        viewModel().play(Station(9, "Gone", listOf("http://gone.example/")))

        assertEquals(emptyList<String>(), playback.commands)
    }

    @Test
    fun `a failed read says so and does not end the app`() = runTest(mainDispatcher.dispatcher) {
        stations.failure = IllegalStateException("The database is closed")

        val state = viewModel().state.value

        assertNull(state.stations)
        assertEquals(true, state.failed)
    }

    @Test
    fun `a failed remove keeps the list and says so`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        stations.failure = IllegalStateException("The database is closed")

        viewModel.remove(alpha)

        assertEquals(RadioUiState(listOf(alpha, beta), failed = true), viewModel.state.value)
    }
}
