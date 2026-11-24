/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Every folder that holds audio, with a tick on the hidden ones. A tap changes one folder at once. */
@Composable
internal fun FoldersDialog(
    folders: List<String>,
    hidden: Set<String>,
    onHide: (path: String, hide: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    // A folder that was hidden and holds no song row any more can still be shown again from here.
    val all = (folders + hidden).distinct().sortedBy { it.lowercase() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_hidden_folders)) },
        text = {
            if (all.isEmpty()) {
                Text(stringResource(R.string.settings_no_folders))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = LIST_HEIGHT)) {
                    items(items = all, key = { it }) { path ->
                        // A folder inside a hidden folder is hidden with it, and only that folder can show it again.
                        val insideHidden = hidden.any { path.startsWith("$it/") }
                        val isHidden = path in hidden || insideHidden
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(folderLabel(path)) },
                            // The whole row is the switch. The box only shows the state.
                            trailingContent = {
                                Checkbox(checked = isHidden, onCheckedChange = null, enabled = !insideHidden)
                            },
                            modifier = Modifier.toggleable(
                                value = isHidden,
                                enabled = !insideHidden,
                                role = Role.Checkbox,
                                onValueChange = { onHide(path, it) },
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_done)) } },
    )
}

/** The lengths to pick the shortest track from. A pick closes the dialog. */
@Composable
internal fun MinTrackDialog(seconds: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    // A value saved by an older version that is not among the choices is listed too, so it shows as picked.
    val choices = (MIN_TRACK_CHOICES + seconds).distinct().sorted()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_min_track)) },
        text = {
            LazyColumn {
                items(items = choices, key = { it }) { choice ->
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(minTrackLabel(choice)) },
                        leadingContent = { RadioButton(selected = choice == seconds, onClick = null) },
                        modifier = Modifier.selectable(
                            selected = choice == seconds,
                            role = Role.RadioButton,
                            onClick = {
                                onPick(choice)
                                onDismiss()
                            },
                        ),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

/** The history cannot be brought back, so clearing it asks first. */
@Composable
internal fun ClearHistoryDialog(onClear: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_clear_history)) },
        text = { Text(stringResource(R.string.settings_clear_history_question)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onClear()
                },
            ) { Text(stringResource(R.string.settings_clear_history_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

/** The path as a person knows it: without the name the system gives the built-in storage. */
internal fun folderLabel(path: String): String = path.removePrefix(BUILT_IN_STORAGE).ifEmpty { path }

private const val BUILT_IN_STORAGE = "external_primary/"
private val MIN_TRACK_CHOICES = listOf(0, 15, 30, 60, 120)
private val LIST_HEIGHT = 320.dp
