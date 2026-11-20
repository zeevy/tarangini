/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.favourites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.glassBackground
import com.siliconcircuits.tarangini.core.ui.theme.glassTopBarColors
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The Favourites tab. It lists files of the phone, so it sits behind the audio permission like the library. */
@Composable
internal fun FavouritesScreen(modifier: Modifier = Modifier) {
    AudioPermissionGate(modifier = modifier, title = stringResource(R.string.favourites_title)) { LoadedFavourites() }
}

@Composable
private fun LoadedFavourites(viewModel: FavouritesViewModel = hiltViewModel()) {
    val songs by viewModel.songs.state.collectAsStateWithLifecycle()
    FavouritesContent(songs, viewModel.songActions, viewModel.songs::loadMore, viewModel.songs::refresh)
}

/** The list of favourite songs, or a text that says how to add one. */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still marked experimental in Material 3.
@Composable
internal fun FavouritesContent(
    songs: PagedItems<Song>,
    songActions: SongRowActions,
    onMoreSongs: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.glassBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(colors = glassTopBarColors(), title = { Text(stringResource(R.string.favourites_title)) })
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = onRetry) {
                if (songs.items.isEmpty()) {
                    MessageState(
                        title = stringResource(R.string.favourites_empty_title),
                        text = stringResource(R.string.favourites_empty_text),
                        icon = painterResource(CoreUiR.drawable.ic_favourite),
                    )
                } else {
                    PagedColumn(list = songs, key = Song::id, onLoadMore = onMoreSongs) { SongRow(it, songActions) }
                }
            }
        }
    }
}
