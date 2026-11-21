/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistImport
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.feature.library.R

/** The types a playlist file can have. Many file apps give such a file no type, so any file can be picked. */
internal val PLAYLIST_FILE_TYPES = arrayOf("audio/x-mpegurl", "audio/mpegurl", "application/vnd.apple.mpegurl", "*/*")

/** The type a new playlist file is saved with. */
internal const val PLAYLIST_FILE_TYPE = "audio/x-mpegurl"

/** Says how the import or the export of a playlist file ended. Nothing is shown while there is nothing to tell. */
@Composable
internal fun PlaylistFileResult(files: PlaylistFileActions) {
    when (val imported = files.imported) {
        null -> Unit
        is PlaylistImport.Done -> ImportDone(imported, files.onClose)
        PlaylistImport.TooLarge -> Told(R.string.playlist_import, R.string.playlist_import_too_large, files.onClose)
        PlaylistImport.NothingFound -> Told(R.string.playlist_import, R.string.playlist_import_nothing, files.onClose)
        PlaylistImport.Failed -> Told(R.string.playlist_import, R.string.playlist_import_failed, files.onClose)
    }
    when (files.exported) {
        null -> Unit
        true -> Told(R.string.playlist_export, R.string.playlist_export_done, files.onClose)
        false -> Told(R.string.playlist_export, R.string.playlist_export_failed, files.onClose)
    }
}

@Composable
private fun Told(title: Int, text: Int, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(text)) },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(CoreUiR.string.action_ok)) } },
    )
}

/** How many songs were found, and the lines of the file that are no song of the library. */
@Composable
private fun ImportDone(done: PlaylistImport.Done, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(done.name) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(pluralStringResource(R.plurals.playlist_import_done, done.total, done.matched, done.total))
                if (done.notFound.isNotEmpty()) {
                    Text(stringResource(R.string.playlist_import_not_found))
                    done.notFound.forEach { line -> Text(line, style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(CoreUiR.string.action_ok)) } },
    )
}
