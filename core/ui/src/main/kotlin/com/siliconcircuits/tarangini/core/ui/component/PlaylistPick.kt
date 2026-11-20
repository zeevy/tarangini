/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * What "Add to playlist" needs: the playlists to pick from and the two things a pick can do.
 *
 * @property playlists every playlist by name; read only while something shows them
 * @property onAdd puts the song at the end of the playlist
 * @property onCreate makes a playlist with this name and puts the song into it
 */
class PlaylistPick(
    val playlists: StateFlow<List<Playlist>>,
    val onAdd: (songId: Long, playlistId: Long) -> Unit,
    val onCreate: (songId: Long, name: String) -> Unit,
)

/**
 * The pick for a screen whose work runs in [scope]. A change that cannot be saved leaves the
 * playlists as they were. That says enough.
 */
fun PlaylistRepository.pickIn(scope: CoroutineScope): PlaylistPick = PlaylistPick(
    playlists = changes()
        .map { attempt { playlists() } ?: emptyList() }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), emptyList()),
    onAdd = { songId, playlistId -> scope.launch { attempt { add(playlistId, songId) } } },
    onCreate = { songId, name -> scope.launch { attempt { create(name)?.let { made -> add(made, songId) } } } },
)

/** The result, or null after a failure. A cancelled coroutine stays cancelled. */
private suspend fun <T> attempt(block: suspend () -> T): T? =
    runCatching { block() }.onFailure { if (it is CancellationException) throw it }.getOrNull()

private const val STOP_AFTER_MS = 5_000L
