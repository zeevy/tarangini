/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.album

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumArt
import com.siliconcircuits.tarangini.core.ui.component.AlbumArtSize
import com.siliconcircuits.tarangini.core.ui.component.FrameCard
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.component.DetailScaffold
import com.siliconcircuits.tarangini.feature.library.component.ListBody
import com.siliconcircuits.tarangini.feature.library.component.PagedColumn
import com.siliconcircuits.tarangini.feature.library.countText
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
        onShuffle = viewModel::shuffle,
    )
}

/** The album: a card with its cover, name, artist and the Play and Shuffle buttons, then its songs. */
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
    onShuffle: () -> Unit = {},
) {
    val name = title.ifEmpty { stringResource(CoreUiR.string.unknown_album) }
    DetailScaffold(
        title = stringResource(R.string.album_title),
        onBack = onBack,
        count = countText(R.plurals.library_count_songs, total = null, songs),
        modifier = modifier,
        hero = {
            AlbumHero(
                name = name,
                artist = artist.ifEmpty { stringResource(CoreUiR.string.unknown_artist) },
                songs = songs,
                onPlay = { songs.items.firstOrNull()?.let(songActions.onPlay) },
                onShuffle = onShuffle,
            )
        },
    ) {
        ListBody(songs.isLoading, failed = songs.failed && songs.items.isEmpty(), onRetry = onRetry) {
            PagedColumn(songs, Song::id, onMoreSongs) { SongRow(it, songActions) }
        }
    }
}

@Composable
private fun AlbumHero(
    name: String,
    artist: String,
    songs: PagedItems<Song>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    val hasSongs = songs.items.isNotEmpty()
    FrameCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(artworkUri = songs.items.firstOrNull()?.artworkUri, size = AlbumArtSize.Header, name = name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = name,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // The whole length is known only once every song is read.
                if (hasSongs && !songs.hasMore) {
                    Text(
                        text = formatDuration(songs.items.sumOf { it.durationMs }),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = DigitsFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPlay,
                        enabled = hasSongs,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Icon(painterResource(CoreUiR.drawable.ic_play), contentDescription = null, Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.album_play),
                            Modifier.padding(start = 6.dp),
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    OutlinedIconButton(
                        onClick = onShuffle,
                        enabled = hasSongs,
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, lookColors.lampOff),
                    ) {
                        Icon(painterResource(CoreUiR.drawable.ic_shuffle), stringResource(R.string.album_shuffle))
                    }
                }
            }
        }
    }
}
