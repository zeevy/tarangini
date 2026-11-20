/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * Runs [read] now and again each time the library changed, for as long as
 * the ViewModel lives. A scan of many files tells of many changes in a short
 * time: after one read the next one waits a moment, and the changes in
 * between count as one. A read that fails is tried again at the next change.
 */
internal fun ViewModel.readOnLibraryChange(library: LibraryRepository, read: suspend () -> Unit) {
    viewModelScope.launch {
        library.changes().conflate().collect {
            attempt { read() }
            delay(REFRESH_GAP_MS)
        }
    }
}

private const val REFRESH_GAP_MS = 500L
