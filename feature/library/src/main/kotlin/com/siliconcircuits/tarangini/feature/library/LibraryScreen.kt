/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumTile
import com.siliconcircuits.tarangini.core.ui.component.ArtistRow
import com.siliconcircuits.tarangini.core.ui.component.FolderRow
import com.siliconcircuits.tarangini.core.ui.component.LedChip
import com.siliconcircuits.tarangini.core.ui.component.ListCard
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.ScreenHeader
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.component.PagedGrid
import com.siliconcircuits.tarangini.feature.library.genre.GenresTab
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate
import com.siliconcircuits.tarangini.feature.library.playlist.PlaylistsTab

/** The lists of the Library tab. The order here is the order of the chips on screen. */
internal enum class LibraryTab(val title: Int) {
    SONGS(R.string.library_tab_songs),
    ALBUMS(R.string.library_tab_albums),
    ARTISTS(R.string.library_tab_artists),
    GENRES(R.string.library_tab_genres),
    FOLDERS(R.string.library_tab_folders),
    PLAYLISTS(R.string.library_tab_playlists),
}

/** How many songs, albums and artists the library has, for the top line of each list. */
internal data class ListCounts(val songs: Int, val albums: Int, val artists: Int)

/**
 * What the four lists show now.
 *
 * @property folders the top folders, or null until they are read
 * @property sort the order of the songs list
 * @property counts the sizes of the lists, or null until they are read
 */
internal data class LibraryState(
    val songs: PagedItems<Song> = PagedItems(),
    val albums: PagedItems<Album> = PagedItems(),
    val artists: PagedItems<Artist> = PagedItems(),
    val folders: List<Folder>? = null,
    val sort: SongSort = SongSort.TITLE,
    val counts: ListCounts? = null,
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
    val onSettings: () -> Unit = {},
    val onShuffleAll: () -> Unit = {},
)

/** The Library tab. It shows the lists once the audio permission is there, and asks for it before. */
@Composable
internal fun LibraryScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: () -> Unit = {},
) {
    // The lists are their own function, so the ViewModel is made only once the permission is there.
    // Nothing is read or scanned before.
    AudioPermissionGate(modifier = modifier) {
        LibraryLists(onOpenAlbum, onOpenArtist, onOpenGenre, onOpenFolder, onOpenPlaylist, onSearch, onSettings)
    }
}

@Composable
private fun LibraryLists(
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenGenre: (Genre) -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The actions are kept, so a new page of rows does not draw every row on screen again.
    val actions = remember(viewModel, onOpenAlbum, onOpenArtist, onOpenFolder, onSearch, onSettings) {
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
            onSettings = onSettings,
            onShuffleAll = viewModel::shuffleAll,
        )
    }
    LibraryContent(
        lists = state,
        songActions = viewModel.songActions,
        actions = actions,
        playlists = { PlaylistsTab(onOpenPlaylist, it) },
        genres = { GenresTab(onOpenGenre, it) },
    )
}

/**
 * The lists of the library: the header, a row of chips with lamps to pick the list, and the list
 * on its own card with its size and tools at the top.
 *
 * @param genres the Genres list on its card; it is given the modifier that sizes the card
 * @param playlists the Playlists list on its card, the same way
 */
@Composable
internal fun LibraryContent(
    lists: LibraryState,
    songActions: SongRowActions,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    firstTab: LibraryTab = LibraryTab.SONGS,
    genres: @Composable (Modifier) -> Unit = {},
    playlists: @Composable (Modifier) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(firstTab) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 10.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.library_title)) {
            IconButton(onClick = actions.onSearch) {
                Icon(painterResource(CoreUiR.drawable.ic_search), stringResource(CoreUiR.string.action_search))
            }
            IconButton(onClick = actions.onSettings) {
                Icon(painterResource(CoreUiR.drawable.ic_settings), stringResource(R.string.library_settings))
            }
        }
        ListChips(tab) { tab = it }
        val card = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)
        LibraryList(tab, lists, songActions, actions, genres, playlists, card)
    }
}

/** The chips that pick a list. They scroll, so six fit a narrow screen with a large font. */
@Composable
private fun ListChips(tab: LibraryTab, onPick: (LibraryTab) -> Unit) {
    val name = stringResource(R.string.library_lists)
    val lit = remember { BringIntoViewRequester() }
    // A list far to the right, such as Playlists after a restore, scrolls its chip into sight.
    LaunchedEffect(tab) { lit.bringIntoView() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .semantics { contentDescription = name },
    ) {
        LibraryTab.entries.forEach { entry ->
            LedChip(
                text = stringResource(entry.title),
                lit = entry == tab,
                onClick = { onPick(entry) },
                modifier = if (entry == tab) Modifier.bringIntoViewRequester(lit) else Modifier,
            )
        }
    }
}

@Composable
private fun LibraryList(
    tab: LibraryTab,
    lists: LibraryState,
    songActions: SongRowActions,
    actions: LibraryActions,
    genres: @Composable (Modifier) -> Unit,
    playlists: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One scroll place per list, kept here, so a list comes back where it was left.
    val songsScroll = rememberLazyListState()
    val albumsScroll = rememberLazyGridState()
    val artistsScroll = rememberLazyListState()
    val foldersScroll = rememberLazyListState()
    when (tab) {
        LibraryTab.SONGS -> ListCard(
            count = countText(R.plurals.library_count_songs, lists.counts?.songs, lists.songs),
            modifier = modifier,
            tools = {
                IconButton(onClick = actions.onShuffleAll) {
                    Icon(painterResource(CoreUiR.drawable.ic_shuffle), stringResource(R.string.library_shuffle_all))
                }
                SortMenu(lists.sort, actions.onSort)
            },
        ) {
            TabBody(lists.songs, actions) {
                PagedColumn(lists.songs, Song::id, actions.onMoreSongs, state = songsScroll) {
                    SongRow(it, songActions)
                }
            }
        }

        LibraryTab.ALBUMS -> ListCard(
            countText(R.plurals.library_count_albums, lists.counts?.albums, lists.albums),
            modifier,
        ) {
            TabBody(lists.albums, actions) {
                PagedGrid(lists.albums, Album::id, actions.onMoreAlbums, state = albumsScroll) { album ->
                    AlbumTile(album, onClick = { actions.onOpenAlbum(album) })
                }
            }
        }

        LibraryTab.ARTISTS -> ListCard(
            countText(R.plurals.library_count_artists, lists.counts?.artists, lists.artists),
            modifier,
        ) {
            TabBody(lists.artists, actions) {
                PagedColumn(lists.artists, Artist::id, actions.onMoreArtists, state = artistsScroll) { artist ->
                    ArtistRow(artist, onClick = { actions.onOpenArtist(artist) })
                }
            }
        }

        LibraryTab.FOLDERS -> {
            val folders = lists.folders.orEmpty()
            ListCard(
                sizeText(R.plurals.library_count_folders, lists.folders),
                modifier,
            ) {
                TabBody(PagedItems(folders, lists.folders == null), actions) {
                    LazyColumn(state = foldersScroll, modifier = Modifier.fillMaxSize()) {
                        items(items = folders, key = Folder::id) { folder ->
                            FolderRow(folder, onClick = { actions.onOpenFolder(folder) })
                        }
                    }
                }
            }
        }

        // The playlists and the genres have their own state, so a test of the other lists needs none of it.
        LibraryTab.PLAYLISTS -> playlists(modifier)

        LibraryTab.GENRES -> genres(modifier)
    }
}

/** The size of a list that is read in one go, for its top line. It is blank until the list is read. */
@Composable
internal fun sizeText(plural: Int, list: List<*>?): String =
    list?.let { pluralStringResource(plural, it.size, it.size.toString()) }.orEmpty()

/**
 * The size of a paged list for its top line. Until the size is read, the rows read so far stand
 * in, with a plus when there are more.
 */
@Composable
internal fun countText(plural: Int, total: Int?, list: PagedItems<*>): String {
    val count = total ?: list.items.size
    val shown = if (total == null && list.hasMore) "$count+" else count.toString()
    return pluralStringResource(plural, count, shown)
}

/** The body of one list: the rows, or what stands in for them while there is nothing to list. */
@Composable
private fun ColumnScope.TabBody(list: PagedItems<*>, actions: LibraryActions, content: @Composable () -> Unit) {
    Box(Modifier.weight(1f, fill = false)) {
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
