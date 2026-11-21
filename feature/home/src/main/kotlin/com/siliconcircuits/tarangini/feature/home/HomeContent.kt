/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumArt
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.PlaylistRow
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.component.rowTextLines
import com.siliconcircuits.tarangini.core.ui.theme.GlassEdge
import com.siliconcircuits.tarangini.core.ui.theme.GlassPanel
import com.siliconcircuits.tarangini.core.ui.theme.Spacing

/** The sections of Home, or the first use text while there is nothing to show yet. */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    songActions: SongRowActions,
    links: HomeLinks,
    modifier: Modifier = Modifier,
) {
    when {
        // The read takes a moment only. A spinner would flash, so nothing is shown.
        state.isLoading -> Unit

        state.isFirstUse -> MessageState(
            title = stringResource(R.string.home_first_title),
            text = stringResource(R.string.home_first_text),
            modifier = modifier,
            icon = painterResource(CoreUiR.drawable.ic_library),
            actionLabel = stringResource(R.string.home_first_open_library),
            onAction = links.onOpenLibrary,
        )

        else -> LazyColumn(modifier = modifier.fillMaxSize()) {
            state.paused?.let { item ->
                item(key = "continue") { ContinueCard(item, links.onResume) }
            }
            if (state.recent.isNotEmpty()) {
                item(key = "recent") { SectionTitle(stringResource(R.string.home_recent)) }
                items(items = state.recent, key = Song::id) { song -> SongRow(song, songActions) }
            }
            if (state.playlists.isNotEmpty()) {
                item(key = "playlists") { SectionTitle(stringResource(R.string.home_playlists)) }
                items(items = state.playlists, key = { "playlist ${it.id}" }) { playlist ->
                    PlaylistRow(playlist, onClick = { links.onOpenPlaylist(playlist) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(start = Spacing.medium, end = Spacing.medium, top = Spacing.large, bottom = Spacing.small)
            .semantics { heading() },
    )
}

/** The item that is loaded in the player while it does not play, with one button to go on. */
@Composable
private fun ContinueCard(item: PlaybackItem, onResume: () -> Unit) {
    Surface(
        color = GlassPanel,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shape = MaterialTheme.shapes.large,
        border = GlassEdge,
        modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(Spacing.medium)) {
            AlbumArt(item.artworkUri)
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.medium)) {
                Text(
                    stringResource(R.string.home_continue),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    item.title ?: stringResource(CoreUiR.string.unknown_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = rowTextLines(),
                    overflow = TextOverflow.Ellipsis,
                )
                item.artist?.takeIf { it.isNotEmpty() }?.let { artist ->
                    Text(
                        artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = rowTextLines(),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // The main button of a glass screen is white with a dark icon.
            FilledIconButton(
                onClick = onResume,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_play),
                    contentDescription = stringResource(CoreUiR.string.action_play),
                )
            }
        }
    }
}
