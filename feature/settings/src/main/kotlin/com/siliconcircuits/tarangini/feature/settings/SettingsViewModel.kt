/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.domain.repository.PlayHistory
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What the Settings screen shows.
 *
 * @property scan what is left out of the library now
 * @property folders every folder that holds audio, hidden ones too
 * @property scanState what the scanner does, for the text under "Rescan now"
 */
internal data class SettingsUiState(
    val scan: ScanSettings = ScanSettings(),
    val folders: List<String> = emptyList(),
    val scanState: ScanState = ScanState.Idle,
)

/** Settings: what the library leaves out, a rescan and the play history. */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val scanner: LibraryScanner,
    private val history: PlayHistory,
) : ViewModel() {
    private val folders = MutableStateFlow<List<String>>(emptyList())

    val state: StateFlow<SettingsUiState> =
        combine(settings.scanSettings(), folders, scanner.state, ::SettingsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), SettingsUiState())

    init {
        // A scan can find a new folder, so the list is read at the start and after every scan.
        viewModelScope.launch {
            scanner.state.collect { now ->
                if (now !is ScanState.Running) attempt { settings.audioFolders() }?.let { folders.value = it }
            }
        }
    }

    /** Saves the shortest track that is kept and scans, so the lists change at once. */
    fun setMinTrackSeconds(seconds: Int) = changeLibrary { settings.setMinTrackSeconds(seconds) }

    /** Hides a folder or shows it again and scans, so the lists change at once. */
    fun setFolderHidden(path: String, hidden: Boolean) = changeLibrary { settings.setFolderHidden(path, hidden) }

    fun rescan() = changeLibrary { }

    /** Forgets every play and every play count. */
    fun clearHistory() {
        viewModelScope.launch { attempt { history.clear() } }
    }

    private fun changeLibrary(save: suspend () -> Unit) {
        viewModelScope.launch {
            attempt {
                save()
                scanner.scan()
            }
        }
    }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}

/** The result, or null after a failure. A cancelled coroutine stays cancelled. */
private suspend fun <T> attempt(block: suspend () -> T): T? =
    runCatching { block() }.onFailure { if (it is CancellationException) throw it }.getOrNull()
