/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedList
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import com.siliconcircuits.tarangini.feature.library.list.readOnLibraryChange
import com.siliconcircuits.tarangini.feature.library.navigation.ArtistRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One artist: the albums, then the songs by title. */
@HiltViewModel
internal class ArtistViewModel @Inject constructor(
    saved: SavedStateHandle,
    library: LibraryRepository,
    actions: SongActions,
) : ViewModel() {
    private val artistId: Long = checkNotNull(saved[ArtistRoute.ARTIST_ID])

    val name: String = saved.get<String>(ArtistRoute.NAME).orEmpty()

    private val artistAlbums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = artistAlbums.asStateFlow()

    val songs = PagedList(viewModelScope, Song::id) { library.songsOfArtist(artistId, it) }

    /** A tap plays the song with all songs of the artist as the queue. */
    val songActions = actions.rowActions(viewModelScope) { library.songsOfArtist(artistId, it) }

    init {
        readOnLibraryChange(library) {
            songs.refresh()
            artistAlbums.value = library.albumsOfArtist(artistId)
        }
    }
}
