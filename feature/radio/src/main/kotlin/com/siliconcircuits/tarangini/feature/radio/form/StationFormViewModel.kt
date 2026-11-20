/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.repository.StationRepository
import com.siliconcircuits.tarangini.core.domain.station.AddressFault
import com.siliconcircuits.tarangini.core.domain.station.MAX_STATION_NAME
import com.siliconcircuits.tarangini.core.domain.station.MAX_STREAM_ADDRESS
import com.siliconcircuits.tarangini.core.domain.station.NameFault
import com.siliconcircuits.tarangini.core.domain.station.StreamTest
import com.siliconcircuits.tarangini.core.domain.station.StreamTester
import com.siliconcircuits.tarangini.core.domain.station.checkStationName
import com.siliconcircuits.tarangini.core.domain.station.checkStreamAddress
import com.siliconcircuits.tarangini.feature.radio.attempt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where the try of the address stands. */
internal sealed interface TestState {
    data object None : TestState

    data object Running : TestState

    data class Done(val result: StreamTest) : TestState
}

/**
 * What the station form shows next to its two text fields.
 *
 * @property isEdit true when a stored station is changed
 * @property nameFault what is wrong with the name, shown after a tap on Save
 * @property addressFault what is wrong with the address, shown after a tap on Save or Test
 * @property isSaved true once the station is stored. The screen closes then
 */
internal data class StationFormUiState(
    val isEdit: Boolean = false,
    val nameFault: NameFault? = null,
    val addressFault: AddressFault? = null,
    val test: TestState = TestState.None,
    val saveFailed: Boolean = false,
    val isSaved: Boolean = false,
)

/** The form that adds a station by name and stream address, or changes a stored one. */
@HiltViewModel
internal class StationFormViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val stations: StationRepository,
    private val tester: StreamTester,
) : ViewModel() {
    private val stationId: Long? = savedState.get<Long>(STATION_ID)?.takeIf { it > 0 }

    /** The texts of the two fields. They are Compose state: a text field needs its text back at once. */
    var name by mutableStateOf("")
        private set
    var address by mutableStateOf("")
        private set

    private val shown = MutableStateFlow(StationFormUiState(isEdit = stationId != null))
    val state: StateFlow<StationFormUiState> = shown.asStateFlow()
    private var testing: Job? = null
    private var saving: Job? = null

    init {
        if (stationId != null) viewModelScope.launch { fill(stationId) }
    }

    private suspend fun fill(id: Long) {
        val station = attempt { stations.station(id) }.getOrNull() ?: return
        // The person may have typed already while the station was read.
        if (name.isEmpty() && address.isEmpty()) {
            name = station.name
            address = station.streamUrls.firstOrNull().orEmpty()
        }
    }

    fun onNameChange(new: String) {
        name = new.take(MAX_STATION_NAME)
        shown.value = shown.value.copy(nameFault = null, saveFailed = false)
    }

    /** A new address makes the last try say nothing about it, so the result goes. */
    fun onAddressChange(new: String) {
        address = new.take(MAX_STREAM_ADDRESS)
        testing?.cancel()
        shown.value = shown.value.copy(addressFault = null, test = TestState.None, saveFailed = false)
    }

    /** Tries the address and shows what answered. An address that does not pass the check is not tried. */
    fun test() {
        testing?.cancel()
        val fault = checkStreamAddress(address)
        shown.value =
            shown.value.copy(addressFault = fault, test = if (fault == null) TestState.Running else TestState.None)
        if (fault == null) {
            val tried = address
            testing = viewModelScope.launch {
                val result = tester.test(tried)
                shown.value = shown.value.copy(test = TestState.Done(result))
            }
        }
    }

    /**
     * Saves when name and address pass. The station is saved whatever a try said: a station can be
     * off air for a while.
     */
    fun save() {
        val nameFault = checkStationName(name)
        val addressFault = checkStreamAddress(address)
        shown.value = shown.value.copy(nameFault = nameFault, addressFault = addressFault, saveFailed = false)
        // A second tap while the first save runs must not store the station twice.
        if (nameFault == null && addressFault == null && saving?.isActive != true) {
            saving = viewModelScope.launch { store() }
        }
    }

    private suspend fun store() {
        val taken = attempt { stations.userStations() }.getOrDefault(emptyList())
            .any { it.id != stationId && address.trim() in it.streamUrls }
        if (taken) {
            shown.value = shown.value.copy(addressFault = AddressFault.ALREADY_SAVED)
        } else {
            val saved = attempt { stations.save(stationId, name, address) }.isSuccess
            shown.value = shown.value.copy(isSaved = saved, saveFailed = !saved)
        }
    }

    companion object {
        /** The name of the route value with the id of the station to change. */
        const val STATION_ID = "stationId"
    }
}
