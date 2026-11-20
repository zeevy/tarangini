/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.browse

import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlayHistory
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** The lists a person makes. A change of one of them concerns only its own lists in the car. */
internal class PersonalLists(
    val favourites: FavouriteRepository,
    val history: PlayHistory,
    val playlists: PlaylistRepository,
)

/**
 * Tells the car to read its lists again after the library changed, so a
 * new song shows in the car without a restart.
 *
 * @param opened the ids of the lists the car has read
 * @param notify tells the car that the list with this id changed
 */
internal class LibraryChangeNotices(
    private val library: LibraryRepository,
    private val personal: PersonalLists,
    private val scope: CoroutineScope,
    private val opened: () -> Collection<String>,
    private val notify: (String) -> Unit,
) {
    private var jobs: List<Job> = emptyList()

    /** Starts to watch the library and the lists a person makes. */
    fun start() {
        // The first value of each is the state at the start, which the car has not read yet.
        jobs = listOf(
            // A scan of many files changes the library many times. The car is told once, after the last change.
            scope.launch { library.changes().drop(1).collectLatest { tellAfterQuiet { true } } },
            // A new mark or a counted play changes one list only. The list the driver looks at is
            // not read again for it.
            scope.launch { personal.favourites.songIds().drop(1).collectLatest { tellAfterQuiet(FAVOURITES::equals) } },
            scope.launch { personal.history.changes().drop(1).collectLatest { tellAfterQuiet(RECENT::equals) } },
            scope.launch {
                personal.playlists.changes().drop(1).collectLatest { tellAfterQuiet { it.startsWith(PLAYLIST) } }
            },
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
        val RECENT = MediaId.Recent.encode()

        // The list of playlists and every playlist start with this word.
        val PLAYLIST = MediaId.Playlists.encode().dropLast(1)
    }
}
