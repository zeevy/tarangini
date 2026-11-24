/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.artist

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumRow
import com.siliconcircuits.tarangini.core.ui.component.SectionTitle
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.DetailScaffold
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.countText
import com.siliconcircuits.tarangini.feature.library.list.PagedItems
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

/** The screen of one artist. */
@Composable
internal fun ArtistScreen(onBack: () -> Unit, onOpenAlbum: (Album) -> Unit, modifier: Modifier = Modifier) {
    AudioPermissionGate(modifier = modifier) { LoadedArtist(onBack, onOpenAlbum) }
}

@Composable
private fun LoadedArtist(
    onBack: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val songs by viewModel.songs.state.collectAsStateWithLifecycle()
    ArtistContent(
        name = viewModel.name,
        albums = albums,
        songs = songs,
        songActions = viewModel.songActions,
        onMoreSongs = viewModel.songs::loadMore,
        onRetry = viewModel.songs::refresh,
        onOpenAlbum = onOpenAlbum,
        onBack = onBack,
    )
}

@Composable
internal fun ArtistContent(
    name: String,
    albums: List<Album>,
    songs: PagedItems<Song>,
    songActions: SongRowActions,
    onMoreSongs: () -> Unit,
    onRetry: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailScaffold(
        title = name.ifEmpty { stringResource(CoreUiR.string.unknown_artist) },
        onBack = onBack,
        count = countText(R.plurals.library_count_songs, total = null, songs),
        modifier = modifier,
    ) {
        ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = onRetry) {
            PagedColumn(
                list = songs,
                key = Song::id,
                onLoadMore = onMoreSongs,
                header = {
                    if (albums.isNotEmpty()) {
                        item(key = "albums") { SectionTitle(stringResource(CoreUiR.string.section_albums)) }
                        // An album and a song can have the same number, so the album keys get a name in front.
                        items(items = albums, key = { "album-${it.id}" }) { album ->
                            AlbumRow(album, onClick = { onOpenAlbum(album) })
                        }
                    }
                    item(key = "songs") { SectionTitle(stringResource(CoreUiR.string.section_songs)) }
                },
            ) { SongRow(it, songActions) }
        }
    }
}
