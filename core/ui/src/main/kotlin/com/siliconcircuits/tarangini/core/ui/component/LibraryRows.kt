/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R

/**
 * One song in a list: the cover picture, the title, the artist with the
 * length, and a menu. A tap plays it. Every list of songs uses this row, so
 * a song looks and behaves the same everywhere.
 */
@Composable
fun SongRow(song: Song, actions: SongRowActions, modifier: Modifier = Modifier) {
    val artist = song.artist.ifEmpty { stringResource(R.string.unknown_artist) }
    val length = formatDuration(song.durationMs)
    val favourites by actions.favourites.collectAsState()
    val isFavourite = song.id in favourites
    ListItem(
        colors = clearRowColors(),
        headlineContent = { OneLine(song.title) },
        supportingContent = {
            OneLine(if (length.isEmpty()) artist else stringResource(R.string.row_two_parts, artist, length))
        },
        leadingContent = { AlbumArt(song.artworkUri) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFavourite) {
                    Icon(
                        painter = painterResource(R.drawable.ic_favourite_filled),
                        contentDescription = stringResource(R.string.song_is_favourite),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(FAVOURITE_MARK),
                    )
                }
                SongMenu(song, actions, isFavourite)
            }
        },
        modifier = modifier.clickable(onClickLabel = stringResource(R.string.action_play)) { actions.onPlay(song) },
    )
}

@Composable
private fun SongMenu(song: Song, actions: SongRowActions, isFavourite: Boolean) {
    var open by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    actions.playlists?.takeIf { adding }?.let { pick -> AddToPlaylistDialog(pick, song.id) { adding = false } }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.song_menu, song.title),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.song_play_next)) },
                onClick = {
                    open = false
                    actions.onPlayNext(song)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.song_add_to_queue)) },
                onClick = {
                    open = false
                    actions.onAddToQueue(song)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isFavourite) R.string.song_remove_favourite else R.string.song_add_favourite,
                        ),
                    )
                },
                onClick = {
                    open = false
                    actions.onSetFavourite(song, !isFavourite)
                },
            )
            if (actions.playlists != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_add_to_playlist)) },
                    onClick = {
                        open = false
                        adding = true
                    },
                )
            }
        }
    }
}

/** One album in a list: the title, the artist and how many songs. A tap opens it. */
@Composable
fun AlbumRow(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val artist = album.artist.ifEmpty { stringResource(R.string.unknown_artist) }
    val count = pluralStringResource(R.plurals.song_count, album.songCount, album.songCount)
    ListItem(
        colors = clearRowColors(),
        headlineContent = { OneLine(album.title.ifEmpty { stringResource(R.string.unknown_album) }) },
        supportingContent = { OneLine(stringResource(R.string.row_two_parts, artist, count)) },
        leadingContent = { AlbumArt(artworkUri = null) },
        modifier = modifier.opens(onClick),
    )
}

/** One artist in a list: the name and how many songs. A tap opens the artist. */
@Composable
fun ArtistRow(artist: Artist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        colors = clearRowColors(),
        headlineContent = { OneLine(artist.name.ifEmpty { stringResource(R.string.unknown_artist) }) },
        supportingContent = { OneLine(pluralStringResource(R.plurals.song_count, artist.songCount, artist.songCount)) },
        leadingContent = { IconTile(painterResource(R.drawable.ic_artist)) },
        modifier = modifier.opens(onClick),
    )
}

/** One folder in a list. A tap opens it. */
@Composable
fun FolderRow(folder: Folder, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        colors = clearRowColors(),
        headlineContent = { OneLine(folder.name) },
        leadingContent = { IconTile(painterResource(R.drawable.ic_folder)) },
        modifier = modifier.opens(onClick),
    )
}

/** Makes a row a button that a screen reader announces as "open". */
@Composable
private fun Modifier.opens(onClick: () -> Unit): Modifier =
    clickable(onClickLabel = stringResource(R.string.action_open), onClick = onClick)

/** A text of a row. A long one is cut with dots, so every row has the same height. */
@Composable
internal fun OneLine(text: String) {
    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A picture of the kind of row, on the same tile a cover picture has, so all rows line up. */
@Composable
internal fun IconTile(icon: Painter) {
    Box(
        modifier = Modifier
            .size(AlbumArtSize.List)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A row with no colour of its own, so the background of the screen shows through the list. */
@Composable
internal fun clearRowColors(): ListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

/** The small heart of a row whose song is a favourite. */
private val FAVOURITE_MARK = 18.dp
