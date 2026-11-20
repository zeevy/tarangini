/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.feature.library.component.DetailScaffold
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The screen of one album. */
@Composable
internal fun AlbumScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    AudioPermissionGate(modifier = modifier) { LoadedAlbum(onBack) }
}

@Composable
private fun LoadedAlbum(onBack: () -> Unit, viewModel: AlbumViewModel = hiltViewModel()) {
    val songs by viewModel.songs.state.collectAsStateWithLifecycle()
    AlbumContent(
        title = viewModel.title,
        artist = viewModel.artist,
        songs = songs,
        songActions = viewModel.songActions,
        onMoreSongs = viewModel.songs::loadMore,
        onRetry = viewModel.songs::refresh,
        onBack = onBack,
    )
}

@Composable
internal fun AlbumContent(
    title: String,
    artist: String,
    songs: PagedItems<Song>,
    songActions: SongRowActions,
    onMoreSongs: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailScaffold(
        title = title.ifEmpty { stringResource(CoreUiR.string.unknown_album) },
        subtitle = artist.ifEmpty { stringResource(CoreUiR.string.unknown_artist) },
        onBack = onBack,
        modifier = modifier,
    ) {
        ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = onRetry) {
            PagedColumn(songs, Song::id, onMoreSongs) { SongRow(it, songActions) }
        }
    }
}
