/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.model.MAX_PLAYLIST_NAME
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.checkPlaylistName
import com.siliconcircuits.tarangini.core.ui.R

/**
 * Asks which playlist a song goes into. A tap on a playlist adds the song and closes the dialog.
 * The field below makes a new playlist with the song in it.
 */
@Composable
fun AddToPlaylistDialog(pick: PlaylistPick, songId: Long, onDismiss: () -> Unit) {
    val playlists by pick.playlists.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    val create = {
        if (checkPlaylistName(name) != null) {
            pick.onCreate(songId, name)
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.song_add_to_playlist)) },
        text = {
            Column {
                LazyColumn(modifier = Modifier.heightIn(max = LIST_HEIGHT)) {
                    items(items = playlists, key = Playlist::id) { playlist ->
                        ListItem(
                            colors = clearRowColors(),
                            headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.clickable {
                                pick.onAdd(songId, playlist.id)
                                onDismiss()
                            },
                        )
                    }
                }
                NameField(name, { name = it }, stringResource(R.string.playlist_new), onDone = create)
            }
        },
        confirmButton = {
            TextButton(onClick = create, enabled = checkPlaylistName(name) != null) {
                Text(stringResource(R.string.playlist_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Asks for the name of a playlist: a new one, or another name for one that exists.
 *
 * @param onConfirm gets the name once it is one a playlist can have
 */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val confirm = {
        if (checkPlaylistName(name) != null) {
            onConfirm(name)
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { NameField(name, { name = it }, stringResource(R.string.playlist_name), onDone = confirm) },
        confirmButton = {
            TextButton(onClick = confirm, enabled = checkPlaylistName(name) != null) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun NameField(name: String, onChange: (String) -> Unit, label: String, onDone: () -> Unit) {
    OutlinedTextField(
        value = name,
        // A longer name could not be saved, so it cannot be typed.
        onValueChange = { onChange(it.take(MAX_PLAYLIST_NAME)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The most room the list of playlists takes in the dialog. More playlists scroll. */
private val LIST_HEIGHT = 240.dp
