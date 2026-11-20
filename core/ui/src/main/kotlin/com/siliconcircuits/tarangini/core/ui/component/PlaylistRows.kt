/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R

/** One playlist in a list: its name and how many songs it has. A tap opens it. */
@Composable
fun PlaylistRow(playlist: Playlist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        colors = clearRowColors(),
        headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(pluralStringResource(R.plurals.song_count, playlist.songCount, playlist.songCount))
        },
        leadingContent = { Icon(painter = painterResource(R.drawable.ic_queue), contentDescription = null) },
        modifier = modifier.clickable(onClickLabel = stringResource(R.string.action_open), onClick = onClick),
    )
}

/**
 * One place in a playlist: the song, and a menu with what can be done with the place. A tap plays
 * the playlist from this song on.
 *
 * @param menu the entries of the menu, each a label and what it does
 */
@Composable
fun PlaylistSongRow(
    song: Song,
    onPlay: () -> Unit,
    menu: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    ListItem(
        colors = clearRowColors(),
        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                song.artist.ifEmpty { stringResource(R.string.unknown_artist) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { AlbumArt(song.artworkUri) },
        trailingContent = {
            Box {
                IconButton(onClick = { open = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = stringResource(R.string.song_menu, song.title),
                    )
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    menu.forEach { (label, action) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                open = false
                                action()
                            },
                        )
                    }
                }
            }
        },
        modifier = modifier.clickable(onClickLabel = stringResource(R.string.action_play), onClick = onPlay),
    )
}
