/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedList
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import com.siliconcircuits.tarangini.feature.library.list.readOnLibraryChange
import com.siliconcircuits.tarangini.feature.library.navigation.AlbumRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** One album: its songs in album order. */
@HiltViewModel
internal class AlbumViewModel @Inject constructor(
    saved: SavedStateHandle,
    library: LibraryRepository,
    actions: SongActions,
) : ViewModel() {
    private val albumId: Long = checkNotNull(saved[AlbumRoute.ALBUM_ID])

    val title: String = saved.get<String>(AlbumRoute.TITLE).orEmpty()
    val artist: String = saved.get<String>(AlbumRoute.ARTIST).orEmpty()

    val songs = PagedList(viewModelScope, Song::id) { library.songsOfAlbum(albumId, it) }

    /** A tap plays the song with the whole album as the queue. */
    val songActions = actions.rowActions(viewModelScope, QueueSource.named(QueueSourceKind.ALBUM, title)) {
        library.songsOfAlbum(albumId, it)
    }

    init {
        readOnLibraryChange(library) { songs.refresh() }
    }
}
