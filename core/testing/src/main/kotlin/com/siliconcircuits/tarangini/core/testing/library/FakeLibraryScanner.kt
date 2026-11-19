/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.library.ScanSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A library scanner for tests. A scan ends at once with [nextOutcome]. A test
 * sets [state] by hand to show a screen a scan that is still running.
 */
class FakeLibraryScanner : LibraryScanner {
    /** What the next call of [scan] returns. */
    var nextOutcome: ScanOutcome = ScanOutcome.Success(ScanSummary(added = 0, updated = 0, removed = 0, unchanged = 0))

    /** How many times [scan] was called. */
    var scanCount = 0
        private set

    override val state = MutableStateFlow<ScanState>(ScanState.Idle)

    override suspend fun scan(): ScanOutcome {
        scanCount++
        state.value = ScanState.Finished(nextOutcome)
        return nextOutcome
    }
}
