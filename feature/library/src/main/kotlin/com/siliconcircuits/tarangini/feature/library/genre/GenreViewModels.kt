/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.genre

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.GenreRepository
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.feature.library.list.PagedList
import com.siliconcircuits.tarangini.feature.library.list.SongActions
import com.siliconcircuits.tarangini.feature.library.list.readOnLibraryChange
import com.siliconcircuits.tarangini.feature.library.navigation.GenreRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The Genres list of the Library tab. */
@HiltViewModel
internal class GenresViewModel @Inject constructor(genres: GenreRepository, library: LibraryRepository) : ViewModel() {
    val genres = PagedList(viewModelScope, Genre::id, genres::genres)

    init {
        readOnLibraryChange(library) { this.genres.refresh() }
    }
}

/** One genre: its songs by title. */
@HiltViewModel
internal class GenreViewModel @Inject constructor(
    saved: SavedStateHandle,
    genres: GenreRepository,
    library: LibraryRepository,
    actions: SongActions,
) : ViewModel() {
    private val genreId: Long = checkNotNull(saved[GenreRoute.GENRE_ID])

    val name: String = saved.get<String>(GenreRoute.NAME).orEmpty()

    val songs = PagedList(viewModelScope, Song::id) { genres.songsOfGenre(genreId, it) }

    /** A tap plays the song with all songs of the genre as the queue. */
    val songActions = actions.rowActions(viewModelScope) { genres.songsOfGenre(genreId, it) }

    init {
        readOnLibraryChange(library) { songs.refresh() }
    }
}
