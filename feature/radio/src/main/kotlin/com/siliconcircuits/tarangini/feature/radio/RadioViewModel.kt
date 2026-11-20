/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.repository.StationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * What the Radio tab shows.
 *
 * @property stations the stations the person added, or null while they are not read yet
 * @property failed true when the last read went wrong
 */
internal data class RadioUiState(val stations: List<Station>? = null, val failed: Boolean = false)

/** The Radio tab: the stations the person added. */
@HiltViewModel
internal class RadioViewModel @Inject constructor(
    private val stations: StationRepository,
    private val playback: PlaybackController,
) : ViewModel() {
    private val shown = MutableStateFlow(RadioUiState())
    val state: StateFlow<RadioUiState> = shown.asStateFlow()

    init {
        viewModelScope.launch { stations.changes().onStart { emit(Unit) }.collect { read() } }
    }

    private suspend fun read() {
        shown.value = attempt { stations.userStations() }
            .fold({ RadioUiState(it) }, { RadioUiState(shown.value.stations, failed = true) })
    }

    /** Plays the station. The other stations are the queue, so "next" in the car goes to the next station. */
    fun play(station: Station) {
        val playable = shown.value.stations.orEmpty().filter { it.streamUrls.isNotEmpty() }
        val index = playable.indexOfFirst { it.id == station.id }
        if (index >= 0) playback.play(playable.map { it.streamUrls.first() }, index)
    }

    fun remove(station: Station) {
        viewModelScope.launch {
            // The list is read again when the store says that it changed. A failure shows there.
            attempt { stations.remove(station.id) }.onFailure { shown.value = shown.value.copy(failed = true) }
        }
    }
}

/** Runs a call to the store. A failure comes back as a result, so it cannot end the app. */
internal suspend fun <T> attempt(block: suspend () -> T): Result<T> =
    runCatching { block() }.onFailure { if (it is CancellationException) throw it }
