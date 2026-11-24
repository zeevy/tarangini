/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.favourites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.ListCard
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.ScreenHeader
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.countText
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The Favourites tab. It lists files of the phone, so it sits behind the audio permission like the library. */
@Composable
internal fun FavouritesScreen(modifier: Modifier = Modifier, onSearch: () -> Unit = {}) {
    AudioPermissionGate(modifier = modifier, title = stringResource(R.string.favourites_title)) {
        LoadedFavourites(onSearch)
    }
}

@Composable
private fun LoadedFavourites(onSearch: () -> Unit, viewModel: FavouritesViewModel = hiltViewModel()) {
    val songs by viewModel.songs.state.collectAsStateWithLifecycle()
    val actions = FavouritesActions(viewModel.songs::loadMore, viewModel.songs::refresh, viewModel::shuffle, onSearch)
    FavouritesContent(songs, viewModel.songActions, actions)
}

/** The list of favourite songs on a list card, or a text that says how to add one. */
@Composable
internal fun FavouritesContent(
    songs: PagedItems<Song>,
    songActions: SongRowActions,
    actions: FavouritesActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 10.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.favourites_title)) {
            IconButton(onClick = actions.onSearch) {
                Icon(painterResource(CoreUiR.drawable.ic_search), stringResource(CoreUiR.string.action_search))
            }
        }
        ListCard(
            count = countText(R.plurals.library_count_songs, total = null, songs),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
            tools = {
                if (songs.items.isNotEmpty()) {
                    IconButton(onClick = actions.onShuffle) {
                        Icon(painterResource(CoreUiR.drawable.ic_shuffle), stringResource(R.string.favourites_shuffle))
                    }
                }
            },
        ) {
            ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = actions.onRetry) {
                if (songs.items.isEmpty()) {
                    MessageState(
                        title = stringResource(R.string.favourites_empty_title),
                        text = stringResource(R.string.favourites_empty_text),
                        icon = painterResource(CoreUiR.drawable.ic_favourite),
                    )
                } else {
                    PagedColumn(list = songs, key = Song::id, onLoadMore = actions.onMoreSongs) {
                        SongRow(it, songActions)
                    }
                }
            }
        }
    }
}
