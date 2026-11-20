/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Tells the car to read its lists again after the library changed, so a
 * new song shows in the car without a restart.
 *
 * @param opened the ids of the lists the car has read
 * @param notify tells the car that the list with this id changed
 */
internal class LibraryChangeNotices(
    private val library: LibraryRepository,
    private val scope: CoroutineScope,
    private val opened: () -> Collection<String>,
    private val notify: (String) -> Unit,
) {
    private var job: Job? = null

    /** Starts to watch the library. */
    fun start() {
        // The first value only says that the reading started.
        job = scope.launch {
            library.changes().drop(1).collectLatest {
                // A scan of many files changes the library many times. The car is told once, after the last change.
                delay(QUIET_MS)
                opened().forEach(notify)
            }
        }
    }

    /** Stops the watch. */
    fun end() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val QUIET_MS = 1_000L
    }
}
