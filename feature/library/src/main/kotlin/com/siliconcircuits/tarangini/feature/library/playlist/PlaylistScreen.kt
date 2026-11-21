/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.PlaylistNameDialog
import com.siliconcircuits.tarangini.core.ui.component.PlaylistSongRow
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.DetailScaffold
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The screen of one playlist. It lists files of the phone, so it sits behind the audio permission. */
@Composable
internal fun PlaylistScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    AudioPermissionGate(modifier = modifier) { LoadedPlaylist(onBack) }
}

@Composable
private fun LoadedPlaylist(onBack: () -> Unit, viewModel: PlaylistViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val songs by viewModel.songs.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        PlaylistActions(
            onPlay = viewModel::play,
            onShuffle = viewModel::shuffle,
            onRename = viewModel::rename,
            onDelete = viewModel::delete,
            onRemove = viewModel::remove,
            onMove = viewModel::move,
            onMore = viewModel.songs::loadMore,
            onRetry = viewModel.songs::refresh,
        )
    }
    // A playlist that is gone has no screen. This also closes it after its own "Delete".
    val close by rememberUpdatedState(onBack)
    LaunchedEffect(state.isGone) { if (state.isGone) close() }
    val exported by viewModel.export.result.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PLAYLIST_FILE_TYPE)) { file ->
        file?.let { viewModel.export.to(it.toString()) }
    }
    val files = PlaylistFileActions(
        exported = exported,
        onExport = { picker.launch("${state.name}.m3u") },
        onClose = viewModel.export::close,
    )
    PlaylistContent(state.name, songs, actions, onBack, files = files)
}

/** The songs of a playlist in order, with Play, Shuffle and a menu in the top bar. */
@Composable
internal fun PlaylistContent(
    name: String,
    songs: PagedItems<PlaylistSong>,
    actions: PlaylistActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    files: PlaylistFileActions = PlaylistFileActions(),
) {
    PlaylistFileResult(files)
    DetailScaffold(
        title = name,
        onBack = onBack,
        modifier = modifier,
        actions = { PlaylistBarActions(name, hasSongs = songs.items.isNotEmpty(), actions, files.onExport) },
    ) {
        ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = actions.onRetry) {
            if (songs.items.isEmpty()) {
                MessageState(
                    title = stringResource(R.string.playlist_empty_title),
                    text = stringResource(R.string.playlist_empty_text),
                    icon = painterResource(CoreUiR.drawable.ic_queue),
                )
            } else {
                PagedColumn(list = songs, key = PlaylistSong::itemId, onLoadMore = actions.onMore) { place ->
                    PlaylistSongRow(place.song, onPlay = { actions.onPlay(place) }, menu = placeMenu(place, actions))
                }
            }
        }
    }
}

@Composable
private fun placeMenu(place: PlaylistSong, actions: PlaylistActions): List<Pair<String, () -> Unit>> = listOf(
    stringResource(R.string.playlist_move_up) to { actions.onMove(place, PlaylistMove.UP) },
    stringResource(R.string.playlist_move_down) to { actions.onMove(place, PlaylistMove.DOWN) },
    stringResource(R.string.playlist_move_top) to { actions.onMove(place, PlaylistMove.TOP) },
    stringResource(R.string.playlist_move_end) to { actions.onMove(place, PlaylistMove.END) },
    stringResource(R.string.playlist_remove) to { actions.onRemove(place) },
)

@Composable
private fun RowScope.PlaylistBarActions(
    name: String,
    hasSongs: Boolean,
    actions: PlaylistActions,
    onExport: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    if (hasSongs) {
        IconButton(onClick = { actions.onPlay(null) }) {
            Icon(painterResource(CoreUiR.drawable.ic_play), stringResource(R.string.playlist_play))
        }
        IconButton(onClick = actions.onShuffle) {
            Icon(painterResource(CoreUiR.drawable.ic_shuffle), stringResource(R.string.playlist_shuffle))
        }
    }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(painterResource(CoreUiR.drawable.ic_more), stringResource(R.string.playlist_menu))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.playlist_rename)) },
                onClick = {
                    menuOpen = false
                    renaming = true
                },
            )
            if (hasSongs) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.playlist_export)) },
                    onClick = {
                        menuOpen = false
                        onExport()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.playlist_delete)) },
                onClick = {
                    menuOpen = false
                    deleting = true
                },
            )
        }
    }
    if (renaming) {
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename),
            confirmLabel = stringResource(R.string.playlist_save),
            onConfirm = actions.onRename,
            onDismiss = { renaming = false },
            initial = name,
        )
    }
    if (deleting) DeleteQuestion(name, actions.onDelete) { deleting = false }
}

/** A playlist cannot be brought back, so its removal asks first. */
@Composable
private fun DeleteQuestion(name: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_delete)) },
        text = { Text(stringResource(R.string.playlist_delete_question, name)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onDelete()
                },
            ) { Text(stringResource(R.string.playlist_delete_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.action_cancel)) } },
    )
}
