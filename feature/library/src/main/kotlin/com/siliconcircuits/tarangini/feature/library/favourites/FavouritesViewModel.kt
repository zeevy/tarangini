/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedList
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import com.siliconcircuits.tarangini.feature.library.list.readOnLibraryChange
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** The favourite songs, the one marked last first. */
@HiltViewModel
internal class FavouritesViewModel @Inject constructor(
    favourites: FavouriteRepository,
    library: LibraryRepository,
    actions: SongActions,
) : ViewModel() {
    val songs = PagedList(viewModelScope, Song::id) { favourites.songs(it) }

    /** A tap plays the song with the favourites as the queue. */
    val songActions = actions.rowActions(viewModelScope) { favourites.songs(it) }

    init {
        // A rescan can take the file of a favourite away or bring it back.
        readOnLibraryChange(library) { songs.refresh() }
        // The first set is what the read above shows already.
        viewModelScope.launch { favourites.songIds().drop(1).collect { songs.refresh() } }
    }
}
