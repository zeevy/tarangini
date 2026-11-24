/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.genre

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.GenreRow
import com.siliconcircuits.tarangini.core.ui.component.ListCard
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.DetailScaffold
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.countText
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The Genres page of the Library tab. */
@Composable
internal fun GenresTab(
    onOpenGenre: (Genre) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GenresViewModel = hiltViewModel(),
) {
    val genres by viewModel.genres.state.collectAsStateWithLifecycle()
    GenresContent(genres, onOpenGenre, modifier, viewModel.genres::loadMore, viewModel.genres::refresh)
}

/** Every genre by name, each with the number of its songs, on a list card. */
@Composable
internal fun GenresContent(
    genres: PagedItems<Genre>,
    onOpenGenre: (Genre) -> Unit,
    modifier: Modifier = Modifier,
    onMore: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    ListCard(countText(R.plurals.library_count_genres, total = null, genres), modifier) {
        ListBody(genres.isLoading, failed = genres.failed && genres.items.isEmpty(), onRetry = onRetry) {
            PagedColumn(list = genres, key = Genre::id, onLoadMore = onMore) { genre ->
                GenreRow(genre, onClick = { onOpenGenre(genre) })
            }
        }
    }
}

/** The screen of one genre. It lists files of the phone, so it sits behind the audio permission. */
@Composable
internal fun GenreScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    AudioPermissionGate(modifier = modifier) { LoadedGenre(onBack) }
}

@Composable
private fun LoadedGenre(onBack: () -> Unit, viewModel: GenreViewModel = hiltViewModel()) {
    val songs by viewModel.songs.state.collectAsStateWithLifecycle()
    GenreContent(
        name = viewModel.name,
        songs = songs,
        songActions = viewModel.songActions,
        onBack = onBack,
        onMore = viewModel.songs::loadMore,
        onRetry = viewModel.songs::refresh,
    )
}

/** The songs of a genre by title. */
@Composable
internal fun GenreContent(
    name: String,
    songs: PagedItems<Song>,
    songActions: SongRowActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onMore: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    DetailScaffold(
        title = name.ifEmpty { stringResource(CoreUiR.string.unknown_genre) },
        onBack = onBack,
        count = countText(R.plurals.library_count_songs, total = null, songs),
        modifier = modifier,
    ) {
        ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = onRetry) {
            PagedColumn(list = songs, key = Song::id, onLoadMore = onMore) { SongRow(it, songActions) }
        }
    }
}
