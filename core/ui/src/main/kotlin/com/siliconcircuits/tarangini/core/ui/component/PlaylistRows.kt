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
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R

/** One playlist in a list: its name and how many songs it has. A tap opens it. */
@Composable
fun PlaylistRow(playlist: Playlist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LookRow(
        title = playlist.name,
        sub = pluralStringResource(R.plurals.song_count, playlist.songCount, playlist.songCount),
        onClick = onClick,
        clickLabel = stringResource(R.string.action_open),
        lead = { IconTile(painterResource(R.drawable.ic_queue)) },
        modifier = modifier,
    ) { OpenArrow() }
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
    LookRow(
        title = song.title,
        sub = song.artist.ifEmpty { stringResource(R.string.unknown_artist) },
        onClick = onPlay,
        clickLabel = stringResource(R.string.action_play),
        lead = { AlbumArt(song.artworkUri, name = song.album) },
        modifier = modifier,
        meta = formatDuration(song.durationMs).ifEmpty { null },
    ) {
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
    }
}
