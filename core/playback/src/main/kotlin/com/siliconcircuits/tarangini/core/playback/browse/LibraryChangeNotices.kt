/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
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
    private val favourites: FavouriteRepository,
    private val scope: CoroutineScope,
    private val opened: () -> Collection<String>,
    private val notify: (String) -> Unit,
) {
    private var jobs: List<Job> = emptyList()

    /** Starts to watch the library and the favourites. */
    fun start() {
        // The first value of each is the state at the start, which the car has not read yet.
        jobs = listOf(
            // A scan of many files changes the library many times. The car is told once, after the last change.
            scope.launch { library.changes().drop(1).collectLatest { tellAfterQuiet { true } } },
            // A new mark changes one list only. The list the driver looks at is not read again for it.
            scope.launch { favourites.songIds().drop(1).collectLatest { tellAfterQuiet { it == FAVOURITES } } },
        )
    }

    private suspend fun tellAfterQuiet(concerns: (String) -> Boolean) {
        delay(QUIET_MS)
        opened().filter(concerns).forEach(notify)
    }

    /** Stops the watch. */
    fun end() {
        jobs.forEach(Job::cancel)
        jobs = emptyList()
    }

    private companion object {
        const val QUIET_MS = 1_000L
        val FAVOURITES = MediaId.Favourites.encode()
    }
}
