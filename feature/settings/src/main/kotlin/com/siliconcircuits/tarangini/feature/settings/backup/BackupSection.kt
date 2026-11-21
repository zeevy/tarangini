/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.repository.BackupRestore
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.feature.settings.R
import com.siliconcircuits.tarangini.feature.settings.SettingsGroupTitle
import com.siliconcircuits.tarangini.feature.settings.SettingsRow

/** The Backup group of Settings, with the file pickers of the system. */
@Composable
internal fun BackupSection(viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The file the person chose to read, kept until they say how to read it.
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_FILE_TYPE)) { file ->
        file?.let { viewModel.backUpTo(it.toString()) }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { file ->
        chosen = file?.toString()
    }
    BackupContent(
        state = state,
        actions = BackupActions(
            // A phone with no file picker gets no crash: nothing opens.
            onBackUp = { runCatching { save.launch(BACKUP_FILE_NAME) } },
            onPickFile = { runCatching { open.launch(BACKUP_FILE_TYPES) } },
            onRestore = { replace ->
                chosen?.let { viewModel.restoreFrom(it, replace) }
                chosen = null
            },
            onCancel = { chosen = null },
            onClose = viewModel::close,
        ),
        asksHow = chosen != null,
    )
}

/**
 * The two backup rows and what they have to tell.
 *
 * @param asksHow true while a file is chosen and the person has not said how to read it
 */
@Composable
internal fun BackupContent(state: BackupUiState, actions: BackupActions, asksHow: Boolean = false) {
    SettingsGroupTitle(stringResource(R.string.settings_backup))
    // While a backup is written or read the rows say so and cannot be tapped.
    val busy = stringResource(R.string.backup_busy)
    SettingsRow(
        stringResource(R.string.settings_backup_save),
        if (state.busy) busy else stringResource(R.string.settings_backup_save_text),
        actions.onBackUp.takeUnless { state.busy },
    )
    SettingsRow(
        stringResource(R.string.settings_backup_restore),
        if (state.busy) busy else stringResource(R.string.settings_backup_restore_text),
        actions.onPickFile.takeUnless { state.busy },
    )
    if (asksHow) RestoreChoice(actions)
    when (state.saved) {
        null -> Unit
        true -> Told(R.string.settings_backup_save, stringResource(R.string.backup_saved), actions.onClose)
        false -> Told(R.string.settings_backup_save, stringResource(R.string.backup_save_failed), actions.onClose)
    }
    state.restored?.let { Told(R.string.settings_backup_restore, restoreText(it), actions.onClose) }
}

/** Replace takes things away, so the person chooses with the two ways named and explained. */
@Composable
private fun RestoreChoice(actions: BackupActions) {
    AlertDialog(
        onDismissRequest = actions.onCancel,
        title = { Text(stringResource(R.string.settings_backup_restore)) },
        text = { LongText(stringResource(R.string.backup_restore_question)) },
        // One below the other: three buttons do not fit side by side at a large font.
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = { actions.onRestore(false) }) { Text(stringResource(R.string.backup_restore_add)) }
                TextButton(onClick = { actions.onRestore(true) }) {
                    Text(stringResource(R.string.backup_restore_replace))
                }
                TextButton(onClick = actions.onCancel) { Text(stringResource(CoreUiR.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun Told(title: Int, text: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(title)) },
        text = { LongText(text) },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(CoreUiR.string.action_ok)) } },
    )
}

/** A text of a dialog that can be longer than the dialog is high at a large font, so it scrolls. */
@Composable
private fun LongText(text: String) {
    Text(text, modifier = Modifier.verticalScroll(rememberScrollState()))
}

@Composable
private fun restoreText(result: BackupRestore): String = when (result) {
    is BackupRestore.Done -> {
        val done = stringResource(R.string.backup_restore_done, result.favourites, result.playlists)
        val missing = result.songsNotFound
        if (missing == 0) {
            done
        } else {
            done + " " + pluralStringResource(R.plurals.backup_restore_not_found, missing, missing)
        }
    }

    BackupRestore.TooLarge -> stringResource(R.string.backup_restore_too_large)

    BackupRestore.NotABackup -> stringResource(R.string.backup_restore_not_a_backup)

    BackupRestore.NewerVersion -> stringResource(R.string.backup_restore_newer)

    BackupRestore.NoLibrary -> stringResource(R.string.backup_restore_no_library)

    BackupRestore.Failed -> stringResource(R.string.backup_restore_failed)
}

/** The name a new backup file is offered with. */
private const val BACKUP_FILE_NAME = "tarangini-backup.json"
private const val BACKUP_FILE_TYPE = "application/json"

/** Many file apps give such a file no type, so any file can be picked. The content is checked. */
private val BACKUP_FILE_TYPES = arrayOf(BACKUP_FILE_TYPE, "*/*")
