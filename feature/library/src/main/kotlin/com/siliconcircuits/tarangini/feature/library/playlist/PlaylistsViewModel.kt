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
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistFiles
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
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
    files: PlaylistFiles,
) : ViewModel() {
    /** Makes a playlist from a file. The new playlist shows in the list by itself. */
    val import = PlaylistFileImport(viewModelScope, files)

    private val list = MutableStateFlow(PagedItems<Playlist>())

    /** Every playlist by name. It says "failed" only while there is nothing to show. */
    val state: StateFlow<PagedItems<Playlist>> = list.asStateFlow()

    init {
        // A rescan changes how many songs of a playlist can be played.
        viewModelScope.launch {
            merge(playlists.changes(), library.changes()).collect { read() }
        }
    }

    /** Reads the playlists again after a read that failed. */
    fun retry() {
        viewModelScope.launch { read() }
    }

    private suspend fun read() {
        val now = attempt { playlists.playlists() }.getOrNull()
        list.value = PagedItems(now ?: list.value.items, isLoading = false, failed = now == null)
    }

    /** Makes an empty playlist. A name that is not allowed makes nothing. */
    fun create(name: String) {
        viewModelScope.launch { attempt { playlists.create(name) } }
    }
}
