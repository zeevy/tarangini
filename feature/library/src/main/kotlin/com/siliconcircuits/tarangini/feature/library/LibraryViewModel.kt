/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedList
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import com.siliconcircuits.tarangini.feature.library.list.attempt
import com.siliconcircuits.tarangini.feature.library.list.readOnLibraryChange
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Library tab: all songs, albums and artists, and the top folders. */
@HiltViewModel
internal class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
    private val scanner: LibraryScanner,
    actions: SongActions,
) : ViewModel() {
    private val sort = MutableStateFlow(SongSort.TITLE)
    private val topFolders = MutableStateFlow<List<Folder>?>(null)

    val songs = PagedList(viewModelScope, Song::id) { library.songs(sort.value, it) }
    val albums = PagedList(viewModelScope, Album::id, library::albums)
    val artists = PagedList(viewModelScope, Artist::id, library::artists)

    /** What the four lists show now. */
    val state: StateFlow<LibraryState> =
        combine(songs.state, albums.state, artists.state, topFolders, sort, ::LibraryState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, LibraryState())

    /** A tap plays the song with all songs, in the order shown, as the queue. */
    val songActions = actions.rowActions(viewModelScope) { library.songs(sort.value, it) }

    init {
        // The phone tells nobody when the audio permission is allowed. This screen shows only
        // with the permission, so the first scan after the answer starts here.
        rescan()
        viewModelScope.launch {
            settings.songSort().collect { saved ->
                sort.value = saved
                songs.refresh(fromTop = true)
            }
        }
        readOnLibraryChange(library) { reload() }
    }

    /** Saves the order of the songs list. The list follows when the setting is stored. */
    fun setSort(sort: SongSort) {
        viewModelScope.launch { settings.setSongSort(sort) }
    }

    /** Looks for new and deleted files now. The lists follow by themselves. */
    fun rescan() {
        // A scan that fails must not end the screen. The scanner keeps what went wrong in its state.
        viewModelScope.launch { attempt { scanner.scan() } }
    }

    /** Reads the four lists again, after a read that failed. */
    fun retry() {
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        songs.refresh()
        albums.refresh()
        artists.refresh()
        // Folders that cannot be read show as none, with the button to look again.
        topFolders.value = attempt { library.folders(parentId = null) }.getOrElse { topFolders.value.orEmpty() }
    }
}
