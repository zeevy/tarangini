/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.folder

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.FolderRow
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.DetailScaffold
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The screen of one folder. */
@Composable
internal fun FolderScreen(onBack: () -> Unit, onOpenFolder: (Folder) -> Unit, modifier: Modifier = Modifier) {
    AudioPermissionGate(modifier = modifier) { LoadedFolder(onBack, onOpenFolder) }
}

@Composable
private fun LoadedFolder(
    onBack: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
    viewModel: FolderViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val songs by viewModel.songs.state.collectAsStateWithLifecycle()
    FolderContent(
        name = viewModel.name,
        folders = folders,
        songs = songs,
        songActions = viewModel.songActions,
        onMoreSongs = viewModel.songs::loadMore,
        onRetry = viewModel.songs::refresh,
        onOpenFolder = onOpenFolder,
        onBack = onBack,
    )
}

@Composable
internal fun FolderContent(
    name: String,
    folders: List<Folder>,
    songs: PagedItems<Song>,
    songActions: SongRowActions,
    onMoreSongs: () -> Unit,
    onRetry: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailScaffold(title = name, onBack = onBack, modifier = modifier) {
        ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = onRetry) {
            if (folders.isEmpty() && songs.items.isEmpty()) {
                // The files of a folder can go while its screen is open.
                MessageState(
                    title = stringResource(R.string.library_folder_empty_title),
                    text = stringResource(R.string.library_folder_empty_text),
                    icon = painterResource(CoreUiR.drawable.ic_folder),
                )
            } else {
                PagedColumn(
                    list = songs,
                    key = Song::id,
                    onLoadMore = onMoreSongs,
                    // A folder and a song can have the same number, so the folder keys get a name in front.
                    header = {
                        items(items = folders, key = { "folder-${it.id}" }) { folder ->
                            FolderRow(folder, onClick = { onOpenFolder(folder) })
                        }
                    },
                ) { SongRow(it, songActions) }
            }
        }
    }
}
