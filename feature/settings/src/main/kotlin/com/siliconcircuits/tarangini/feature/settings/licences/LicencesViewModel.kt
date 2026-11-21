/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.licences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.OpenSourceLibrary
import com.siliconcircuits.tarangini.core.domain.repository.LicenceCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A licence text that is open.
 *
 * @property text the full text, or null when the app has none for this licence
 */
internal data class OpenedLicence(val title: String, val text: String?)

/**
 * What the licences screen shows.
 *
 * @property libraries every library in the app by name, or null until they are read
 * @property opened the licence text that is open, or null while the list shows
 */
internal data class LicencesUiState(val libraries: List<OpenSourceLibrary>? = null, val opened: OpenedLicence? = null)

/** The libraries in the app and the text of the licence of each. */
@HiltViewModel
internal class LicencesViewModel @Inject constructor(private val catalog: LicenceCatalog) : ViewModel() {
    private val shown = MutableStateFlow(LicencesUiState())

    val state: StateFlow<LicencesUiState> = shown.asStateFlow()

    init {
        viewModelScope.launch {
            val libraries = attempt { catalog.libraries() }.orEmpty()
            shown.update { it.copy(libraries = libraries) }
        }
    }

    /** Opens the text of the licence of a library. */
    fun open(library: OpenSourceLibrary) {
        viewModelScope.launch {
            val text = attempt { catalog.text(library.licence.id) }
            shown.update { it.copy(opened = OpenedLicence(library.licence.name, text)) }
        }
    }

    /** Goes back to the list. */
    fun close() = shown.update { it.copy(opened = null) }
}

/** The result, or null after a failure. A cancelled coroutine stays cancelled. */
private suspend fun <T> attempt(block: suspend () -> T): T? =
    runCatching { block() }.onFailure { if (it is CancellationException) throw it }.getOrNull()
