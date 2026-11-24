/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.ListCard
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.PlaylistNameDialog
import com.siliconcircuits.tarangini.core.ui.component.PlaylistRow
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.sizeText

/** The Playlists page of the Library tab. */
@Composable
internal fun PlaylistsTab(
    onOpenPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imported by viewModel.import.result.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { file ->
        file?.let { viewModel.import.from(it.toString()) }
    }
    PlaylistsContent(
        playlists = state.items.takeUnless { state.isLoading },
        onOpenPlaylist = onOpenPlaylist,
        onCreate = viewModel::create,
        modifier = modifier,
        failed = state.failed && state.items.isEmpty(),
        onRetry = viewModel::retry,
        files = PlaylistFileActions(
            imported = imported,
            // A phone with no file picker gets no crash: nothing opens.
            onImport = { runCatching { picker.launch(PLAYLIST_FILE_TYPES) } },
            onClose = viewModel.import::close,
        ),
    )
}

/**
 * Every playlist by name on a list card, with the buttons for a new playlist and a playlist file at
 * its top. With no playlist the card says so and offers a new one.
 */
@Composable
internal fun PlaylistsContent(
    playlists: List<Playlist>?,
    onOpenPlaylist: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
    files: PlaylistFileActions = PlaylistFileActions(),
) {
    PlaylistFileResult(files)
    var naming by rememberSaveable { mutableStateOf(false) }
    if (naming) {
        PlaylistNameDialog(
            title = stringResource(CoreUiR.string.playlist_new),
            confirmLabel = stringResource(CoreUiR.string.playlist_create),
            onConfirm = onCreate,
            onDismiss = { naming = false },
        )
    }
    ListCard(
        count = sizeText(R.plurals.library_count_playlists, playlists),
        modifier = modifier,
        tools = {
            IconButton(onClick = { naming = true }) {
                Icon(painterResource(CoreUiR.drawable.ic_add), stringResource(CoreUiR.string.playlist_new))
            }
            IconButton(onClick = files.onImport) {
                Icon(painterResource(CoreUiR.drawable.ic_import), stringResource(R.string.playlist_import))
            }
        },
    ) {
        ListBody(isLoading = playlists == null, failed = failed, onRetry = onRetry) {
            if (playlists.isNullOrEmpty()) {
                MessageState(
                    title = stringResource(R.string.playlists_empty_title),
                    text = stringResource(R.string.playlists_empty_text),
                    actionLabel = stringResource(CoreUiR.string.playlist_new),
                    onAction = { naming = true },
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = playlists, key = Playlist::id) { playlist ->
                        PlaylistRow(playlist, onClick = { onOpenPlaylist(playlist) })
                    }
                }
            }
        }
    }
}
