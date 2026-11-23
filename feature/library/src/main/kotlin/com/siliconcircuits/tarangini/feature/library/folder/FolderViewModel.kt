/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.folder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedList
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import com.siliconcircuits.tarangini.feature.library.list.readOnLibraryChange
import com.siliconcircuits.tarangini.feature.library.navigation.FolderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One folder: the folders directly inside it, then its own songs by file name. */
@HiltViewModel
internal class FolderViewModel @Inject constructor(
    saved: SavedStateHandle,
    library: LibraryRepository,
    actions: SongActions,
) : ViewModel() {
    private val folderId: Long = checkNotNull(saved[FolderRoute.FOLDER_ID])

    val name: String = saved.get<String>(FolderRoute.NAME).orEmpty()

    private val childFolders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = childFolders.asStateFlow()

    val songs = PagedList(viewModelScope, Song::id) { library.songsInFolder(folderId, it) }

    /** A tap plays the song with the songs of this folder as the queue. */
    val songActions = actions.rowActions(viewModelScope, QueueSource.named(QueueSourceKind.FOLDER, name)) {
        library.songsInFolder(folderId, it)
    }

    init {
        readOnLibraryChange(library) {
            songs.refresh()
            childFolders.value = library.folders(folderId)
        }
    }
}
