/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumRow
import com.siliconcircuits.tarangini.core.ui.component.ArtistRow
import com.siliconcircuits.tarangini.core.ui.component.FolderRow
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The lists of the Library tab. The order here is the order of the tabs on screen. */
internal enum class LibraryTab(val title: Int) {
    SONGS(R.string.library_tab_songs),
    ALBUMS(R.string.library_tab_albums),
    ARTISTS(R.string.library_tab_artists),
    FOLDERS(R.string.library_tab_folders),
}

/**
 * What the four lists show now.
 *
 * @property folders the top folders, or null until they are read
 * @property sort the order of the songs list
 */
internal data class LibraryState(
    val songs: PagedItems<Song> = PagedItems(),
    val albums: PagedItems<Album> = PagedItems(),
    val artists: PagedItems<Artist> = PagedItems(),
    val folders: List<Folder>? = null,
    val sort: SongSort = SongSort.TITLE,
)

/** What the Library tab can do besides the actions of a song row. */
internal class LibraryActions(
    val onSort: (SongSort) -> Unit = {},
    val onMoreSongs: () -> Unit = {},
    val onMoreAlbums: () -> Unit = {},
    val onMoreArtists: () -> Unit = {},
    val onOpenAlbum: (Album) -> Unit = {},
    val onOpenArtist: (Artist) -> Unit = {},
    val onOpenFolder: (Folder) -> Unit = {},
    val onRescan: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onSearch: () -> Unit = {},
)

/** The Library tab. It shows the lists once the audio permission is there, and asks for it before. */
@Composable
internal fun LibraryScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The lists are their own function, so the ViewModel is made only once the permission is there.
    // Nothing is read or scanned before.
    AudioPermissionGate(modifier = modifier) { LibraryLists(onOpenAlbum, onOpenArtist, onOpenFolder, onSearch) }
}

@Composable
private fun LibraryLists(
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onSearch: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The actions are kept, so a new page of rows does not draw every row on screen again.
    val actions = remember(viewModel, onOpenAlbum, onOpenArtist, onOpenFolder, onSearch) {
        LibraryActions(
            onSort = viewModel::setSort,
            onMoreSongs = viewModel.songs::loadMore,
            onMoreAlbums = viewModel.albums::loadMore,
            onMoreArtists = viewModel.artists::loadMore,
            onOpenAlbum = onOpenAlbum,
            onOpenArtist = onOpenArtist,
            onOpenFolder = onOpenFolder,
            onRescan = viewModel::rescan,
            onRetry = viewModel::retry,
            onSearch = onSearch,
        )
    }
    LibraryContent(state, viewModel.songActions, actions)
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar and the tab row are still marked experimental in Material 3.
@Composable
internal fun LibraryContent(
    lists: LibraryState,
    songActions: SongRowActions,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    firstTab: LibraryTab = LibraryTab.SONGS,
) {
    var tab by rememberSaveable { mutableStateOf(firstTab) }
    // One scroll place per tab, kept here, so a tab comes back where it was left.
    val songsScroll = rememberLazyListState()
    val albumsScroll = rememberLazyListState()
    val artistsScroll = rememberLazyListState()
    val foldersScroll = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = actions.onSearch) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_search),
                            contentDescription = stringResource(CoreUiR.string.action_search),
                        )
                    }
                    if (tab == LibraryTab.SONGS) SortMenu(lists.sort, actions.onSort)
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // The tab row scrolls, so four tabs fit a narrow screen with a large font.
            PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = Spacing.none) {
                LibraryTab.entries.forEach { entry ->
                    Tab(
                        selected = entry == tab,
                        onClick = { tab = entry },
                        text = { Text(stringResource(entry.title)) },
                    )
                }
            }
            when (tab) {
                LibraryTab.SONGS -> TabBody(lists.songs, actions) {
                    PagedColumn(lists.songs, Song::id, actions.onMoreSongs, state = songsScroll) {
                        SongRow(it, songActions)
                    }
                }

                LibraryTab.ALBUMS -> TabBody(lists.albums, actions) {
                    PagedColumn(lists.albums, Album::id, actions.onMoreAlbums, state = albumsScroll) { album ->
                        AlbumRow(album, onClick = { actions.onOpenAlbum(album) })
                    }
                }

                LibraryTab.ARTISTS -> TabBody(lists.artists, actions) {
                    PagedColumn(lists.artists, Artist::id, actions.onMoreArtists, state = artistsScroll) { artist ->
                        ArtistRow(artist, onClick = { actions.onOpenArtist(artist) })
                    }
                }

                LibraryTab.FOLDERS -> TabBody(PagedItems(lists.folders.orEmpty(), lists.folders == null), actions) {
                    LazyColumn(state = foldersScroll, modifier = Modifier.fillMaxSize()) {
                        items(items = lists.folders.orEmpty(), key = Folder::id) { folder ->
                            FolderRow(folder, onClick = { actions.onOpenFolder(folder) })
                        }
                    }
                }
            }
        }
    }
}

/** The body of one tab: the list, or what stands in for it while there is nothing to list. */
@Composable
private fun TabBody(list: PagedItems<*>, actions: LibraryActions, content: @Composable () -> Unit) {
    ListBody(list.isLoading, failed = list.failed && list.items.isEmpty(), onRetry = actions.onRetry) {
        if (list.items.isEmpty()) {
            MessageState(
                title = stringResource(R.string.library_empty_title),
                text = stringResource(R.string.library_empty_text),
                icon = painterResource(CoreUiR.drawable.ic_library),
                actionLabel = stringResource(R.string.library_empty_scan),
                onAction = actions.onRescan,
            )
        } else {
            content()
        }
    }
}

@Composable
private fun SortMenu(sort: SongSort, onSort: (SongSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_sort),
                contentDescription = stringResource(R.string.library_sort),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SongSort.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(stringResource(entry.label())) },
                    // The whole row is the button. The circle only shows which order is on.
                    leadingIcon = { RadioButton(selected = entry == sort, onClick = null) },
                    onClick = {
                        open = false
                        onSort(entry)
                    },
                )
            }
        }
    }
}

private fun SongSort.label(): Int = when (this) {
    SongSort.TITLE -> R.string.library_sort_title
    SongSort.ARTIST -> R.string.library_sort_artist
    SongSort.ALBUM -> R.string.library_sort_album
    SongSort.DATE_ADDED -> R.string.library_sort_date_added
    SongSort.DURATION -> R.string.library_sort_duration
}
