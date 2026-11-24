/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.Folder
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.ui.R
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/**
 * One song in a list: the cover tile, the title, the artist with the album, the length, and a
 * menu. A tap plays it. Every list of songs uses this row, so a song looks and behaves the same
 * everywhere. The row of the song that plays is lit.
 */
@Composable
fun SongRow(song: Song, actions: SongRowActions, modifier: Modifier = Modifier) {
    val artist = song.artist.ifEmpty { stringResource(R.string.unknown_artist) }
    val favourites by actions.favourites.collectAsState()
    val isFavourite = song.id in favourites
    val playing by actions.playing.collectAsState()
    LookRow(
        title = song.title,
        sub = if (song.album.isEmpty()) artist else stringResource(R.string.row_two_parts, artist, song.album),
        onClick = { actions.onPlay(song) },
        clickLabel = stringResource(R.string.action_play),
        lead = { AlbumArt(song.artworkUri, name = song.album) },
        modifier = modifier,
        meta = formatDuration(song.durationMs).ifEmpty { null },
        isCurrent = song.mediaUri == playing,
    ) {
        if (isFavourite) {
            // The heart says the song is a favourite, and a tap on it takes the mark away.
            IconButton(onClick = { actions.onSetFavourite(song, false) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_favourite_filled),
                    contentDescription = stringResource(R.string.song_is_favourite, song.title),
                    tint = lookColors.heart,
                    modifier = Modifier.size(FAVOURITE_MARK),
                )
            }
        }
        SongMenu(song, actions, isFavourite)
    }
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

/** One album in a list: the cover tile, the title, the artist and how many songs. A tap opens it. */
@Composable
fun AlbumRow(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = album.title.ifEmpty { stringResource(R.string.unknown_album) }
    val artist = album.artist.ifEmpty { stringResource(R.string.unknown_artist) }
    val count = pluralStringResource(R.plurals.song_count, album.songCount, album.songCount)
    LookRow(
        title = title,
        sub = stringResource(R.string.row_two_parts, artist, count),
        onClick = onClick,
        clickLabel = stringResource(R.string.action_open),
        lead = { AlbumArt(artworkUri = null, name = title) },
        modifier = modifier,
    ) { OpenArrow() }
}

/**
 * One album as a tile of a grid: a large cover tile, the title and the artist. A tap opens it.
 * The tile takes the width it is given.
 */
@Composable
fun AlbumTile(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = album.title.ifEmpty { stringResource(R.string.unknown_album) }
    val artist = album.artist.ifEmpty { stringResource(R.string.unknown_artist) }
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = stringResource(R.string.action_open), onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AlbumArt(
            artworkUri = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            size = Dp.Unspecified,
            name = title,
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = lookColors.listText,
            maxLines = rowTextLines(),
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = lookColors.listMuted,
            maxLines = rowTextLines(),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One artist in a list: a round tile with the first letters, the name and how many songs. A tap opens the artist. */
@Composable
fun ArtistRow(artist: Artist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = artist.name.ifEmpty { stringResource(R.string.unknown_artist) }
    LookRow(
        title = name,
        sub = pluralStringResource(R.plurals.song_count, artist.songCount, artist.songCount),
        onClick = onClick,
        clickLabel = stringResource(R.string.action_open),
        lead = { IconTile(icon = null, initials = initialsOf(name), round = true) },
        modifier = modifier,
    ) { OpenArrow() }
}

/** One genre in a list: the name and how many songs. A tap opens the genre. */
@Composable
fun GenreRow(genre: Genre, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LookRow(
        title = genre.name.ifEmpty { stringResource(R.string.unknown_genre) },
        sub = pluralStringResource(R.plurals.song_count, genre.songCount, genre.songCount),
        onClick = onClick,
        clickLabel = stringResource(R.string.action_open),
        lead = { IconTile(painterResource(R.drawable.ic_music_note)) },
        modifier = modifier,
    ) { OpenArrow() }
}

/** One folder in a list. A tap opens it. */
@Composable
fun FolderRow(folder: Folder, onClick: () -> Unit, modifier: Modifier = Modifier) {
    LookRow(
        title = folder.name,
        sub = null,
        onClick = onClick,
        clickLabel = stringResource(R.string.action_open),
        lead = { IconTile(painterResource(R.drawable.ic_folder)) },
        modifier = modifier,
    ) { OpenArrow() }
}

/** A text of a row. A long one is cut with dots after the lines [rowTextLines] allows. */
@Composable
internal fun OneLine(text: String) {
    Text(text = text, maxLines = rowTextLines(), overflow = TextOverflow.Ellipsis)
}

/** A row with no colour of its own, so the background of the screen shows through the list. */
@Composable
internal fun clearRowColors(): ListItemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

/** The small heart of a row whose song is a favourite. */
private val FAVOURITE_MARK = 18.dp
