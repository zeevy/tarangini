/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio.form

import androidx.lifecycle.SavedStateHandle
import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.domain.station.AddressFault
import com.siliconcircuits.tarangini.core.domain.station.MAX_STATION_NAME
import com.siliconcircuits.tarangini.core.domain.station.MAX_STREAM_ADDRESS
import com.siliconcircuits.tarangini.core.domain.station.NameFault
import com.siliconcircuits.tarangini.core.domain.station.StreamTest
import com.siliconcircuits.tarangini.core.domain.station.StreamTestFault
import com.siliconcircuits.tarangini.core.testing.station.FakeStationRepository
import com.siliconcircuits.tarangini.core.testing.station.FakeStreamTester
import com.siliconcircuits.tarangini.feature.radio.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StationFormViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val stored = Station(7, "Radio Bhairavi", listOf("https://bhairavi.example/stream"))
    private val stations = FakeStationRepository(listOf(stored))
    private val tester = FakeStreamTester()

    private fun viewModel(stationId: Long? = null): StationFormViewModel {
        val route = SavedStateHandle(listOfNotNull(stationId?.let { StationFormViewModel.STATION_ID to it }).toMap())
        return StationFormViewModel(route, stations, tester)
    }

    private fun StationFormViewModel.type(name: String, address: String) {
        onNameChange(name)
        onAddressChange(address)
    }

    @Test
    fun `a new station starts with empty fields`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        assertEquals("", viewModel.name)
        assertEquals("", viewModel.address)
        assertEquals(StationFormUiState(), viewModel.state.value)
    }

    @Test
    fun `a good name and address are saved, and the form says that it is done`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.type(" Radio  City ", " http://city.example/live.pls ")

        viewModel.save()

        assertEquals(true, viewModel.state.value.isSaved)
        assertEquals(Station(8, "Radio City", listOf("http://city.example/live.pls")), stations.stations.last())
    }

    @Test
    fun `a bad name or address shows what is wrong and nothing is saved`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.type("  ", "file:///sdcard/a.mp3")

        viewModel.save()

        val state = viewModel.state.value
        assertEquals(NameFault.EMPTY, state.nameFault)
        assertEquals(AddressFault.NOT_HTTP, state.addressFault)
        assertEquals(false, state.isSaved)
        assertEquals(listOf(stored), stations.stations)
    }

    @Test
    fun `typing takes the message of that field away`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.save()

        viewModel.onNameChange("R")
        assertEquals(null, viewModel.state.value.nameFault)
        assertEquals(AddressFault.EMPTY, viewModel.state.value.addressFault)

        viewModel.onAddressChange("h")
        assertEquals(null, viewModel.state.value.addressFault)
    }

    @Test
    fun `very long input is cut while it is typed`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()

        viewModel.type("n".repeat(5_000), "http://a.example/" + "a".repeat(100_000))

        assertEquals(MAX_STATION_NAME, viewModel.name.length)
        assertEquals(MAX_STREAM_ADDRESS, viewModel.address.length)
    }

    @Test
    fun `a stored station fills the form and is saved under its id`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel(stationId = 7)
        assertEquals(true, viewModel.state.value.isEdit)
        assertEquals("Radio Bhairavi", viewModel.name)
        assertEquals("https://bhairavi.example/stream", viewModel.address)

        viewModel.onNameChange("Bhairavi FM")
        viewModel.save()

        assertEquals(listOf(Station(7, "Bhairavi FM", listOf("https://bhairavi.example/stream"))), stations.stations)
    }

    @Test
    fun `a test shows that it runs and then what answered`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        tester.gate = CompletableDeferred()
        tester.answer = StreamTest.Found("audio/mpeg", "City", 128)
        viewModel.onAddressChange("http://city.example/")

        viewModel.test()
        assertEquals(TestState.Running, viewModel.state.value.test)

        tester.gate?.complete(Unit)
        assertEquals(TestState.Done(StreamTest.Found("audio/mpeg", "City", 128)), viewModel.state.value.test)
        assertEquals(listOf("http://city.example/"), tester.tested)
    }

    @Test
    fun `an address that does not pass the check is not tried`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.onAddressChange("ftp://city.example/")

        viewModel.test()

        assertEquals(AddressFault.NOT_HTTP, viewModel.state.value.addressFault)
        assertEquals(TestState.None, viewModel.state.value.test)
        assertEquals(emptyList<String>(), tester.tested)
    }

    @Test
    fun `a new address takes the result away, also one that is still on its way`() =
        runTest(mainDispatcher.dispatcher) {
            val viewModel = viewModel()
            tester.gate = CompletableDeferred()
            viewModel.onAddressChange("http://city.example/")
            viewModel.test()

            viewModel.onAddressChange("http://city.example/2")
            tester.gate?.complete(Unit)

            assertEquals(TestState.None, viewModel.state.value.test)
        }

    @Test
    fun `a station is saved also when the test found nothing`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        tester.answer = StreamTest.Failed(StreamTestFault.NO_ANSWER)
        viewModel.type("City", "http://city.example/")
        viewModel.test()

        viewModel.save()

        assertEquals(true, viewModel.state.value.isSaved)
    }

    @Test
    fun `an address another station has is not saved, and the form says why`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.type("Copy", " https://bhairavi.example/stream ")

        viewModel.save()

        assertEquals(AddressFault.ALREADY_SAVED, viewModel.state.value.addressFault)
        assertEquals(false, viewModel.state.value.isSaved)
        assertEquals(listOf(stored), stations.stations)
    }

    @Test
    fun `two quick taps on Save store one station`() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = viewModel()
        viewModel.type("City", "http://city.example/")

        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf("Radio Bhairavi", "City"), stations.stations.map { it.name })
    }

    @Test
    fun `a failed save says so and the form stays open`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = viewModel()
        viewModel.type("City", "http://city.example/")
        stations.failure = IllegalStateException("The database is closed")

        viewModel.save()

        assertEquals(true, viewModel.state.value.saveFailed)
        assertEquals(false, viewModel.state.value.isSaved)
    }
}
