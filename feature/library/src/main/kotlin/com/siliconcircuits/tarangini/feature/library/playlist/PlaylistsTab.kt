/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.PlaylistNameDialog
import com.siliconcircuits.tarangini.core.ui.component.PlaylistRow
import com.siliconcircuits.tarangini.feature.library.component.ListBody

/** The Playlists page of the Library tab. */
@Composable
internal fun PlaylistsTab(onOpenPlaylist: (Playlist) -> Unit, viewModel: PlaylistsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlaylistsContent(
        playlists = state.items.takeUnless { state.isLoading },
        onOpenPlaylist = onOpenPlaylist,
        onCreate = viewModel::create,
        failed = state.failed && state.items.isEmpty(),
        onRetry = viewModel::retry,
    )
}

/** "New playlist" first, then every playlist by name. */
@Composable
internal fun PlaylistsContent(
    playlists: List<Playlist>?,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    var naming by rememberSaveable { mutableStateOf(false) }
    if (naming) {
        PlaylistNameDialog(
            title = stringResource(CoreUiR.string.playlist_new),
            confirmLabel = stringResource(CoreUiR.string.playlist_create),
            onConfirm = onCreate,
            onDismiss = { naming = false },
        )
    }
    ListBody(isLoading = playlists == null, failed = failed, onRetry = onRetry) {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            item(key = "new") {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(CoreUiR.string.playlist_new)) },
                    leadingContent = {
                        Icon(painter = painterResource(CoreUiR.drawable.ic_add), contentDescription = null)
                    },
                    modifier = Modifier.clickable { naming = true },
                )
            }
            items(items = playlists.orEmpty(), key = Playlist::id) { playlist ->
                PlaylistRow(playlist, onClick = { onOpenPlaylist(playlist) })
            }
        }
    }
}
