/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.feature.library.list.attempt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** The list of playlists in the Library tab. */
@HiltViewModel
internal class PlaylistsViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    library: LibraryRepository,
) : ViewModel() {
    private val list = MutableStateFlow<List<Playlist>?>(null)

    /** Every playlist by name. Null until the first read is done. */
    val state: StateFlow<List<Playlist>?> = list.asStateFlow()

    init {
        // A rescan changes how many songs of a playlist can be played.
        viewModelScope.launch {
            merge(playlists.changes(), library.changes()).collect {
                list.value = attempt { playlists.playlists() }.getOrDefault(list.value.orEmpty())
            }
        }
    }

    /** Makes an empty playlist. A name that is not allowed makes nothing. */
    fun create(name: String) {
        viewModelScope.launch { attempt { playlists.create(name) } }
    }
}
