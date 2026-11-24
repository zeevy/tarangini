/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.domain.library.ScanFailure
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.model.Look

/**
 * The rows of Settings in groups: the look, Library, Playback, History, Backup (the [backup] slot)
 * and About.
 */
@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    version: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    firstDialog: SettingsDialog = SettingsDialog.NONE,
    backup: @Composable () -> Unit = {},
) {
    var dialog by rememberSaveable { mutableStateOf(firstDialog) }
    val close = { dialog = SettingsDialog.NONE }
    when (dialog) {
        SettingsDialog.FOLDERS -> FoldersDialog(
            state.folders,
            state.scan.hiddenFolders,
            actions.onSetFolderHidden,
            close,
        )

        SettingsDialog.MIN_TRACK -> MinTrackDialog(state.scan.minTrackSeconds, actions.onSetMinTrackSeconds, close)

        SettingsDialog.CLEAR_HISTORY -> ClearHistoryDialog(actions.onClearHistory, close)

        SettingsDialog.NONE -> Unit
    }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
        SettingsGroupTitle(stringResource(R.string.settings_look))
        LookPicker(state.look, actions.onSetLook)

        SettingsGroupTitle(stringResource(R.string.settings_library))
        val hidden = state.scan.hiddenFolders.size
        SettingsRow(
            title = stringResource(R.string.settings_hidden_folders),
            note = null,
            onClick = { dialog = SettingsDialog.FOLDERS },
            value = if (hidden == 0) {
                stringResource(R.string.settings_hidden_folders_none)
            } else {
                pluralStringResource(R.plurals.settings_hidden_folders_count, hidden, hidden)
            },
            isFirst = true,
        )
        SettingsRow(
            title = stringResource(R.string.settings_min_track),
            note = stringResource(R.string.settings_min_track_note),
            onClick = { dialog = SettingsDialog.MIN_TRACK },
            // The row has its own note, so "Off" says enough there; the dialog keeps the long text.
            value = if (state.scan.minTrackSeconds <= 0) {
                stringResource(R.string.settings_min_track_off_short)
            } else {
                minTrackLabel(state.scan.minTrackSeconds)
            },
        )
        SettingsRow(stringResource(R.string.settings_rescan), scanText(state.scanState), actions.onRescan)

        SettingsGroupTitle(stringResource(R.string.settings_sound))
        SettingsSwitchRow(
            title = stringResource(R.string.settings_levelling),
            note = stringResource(R.string.settings_levelling_text),
            on = state.levelling,
            onChange = actions.onSetLevelling,
            isFirst = true,
        )

        SettingsGroupTitle(stringResource(R.string.settings_history))
        SettingsRow(
            title = stringResource(R.string.settings_clear_history),
            note = stringResource(R.string.settings_clear_history_text),
            onClick = { dialog = SettingsDialog.CLEAR_HISTORY },
            isFirst = true,
            isDanger = true,
        )

        // The backup rows have their own state, so a test of the other rows needs none of it.
        backup()

        SettingsGroupTitle(stringResource(R.string.settings_about))
        SettingsRow(
            stringResource(R.string.settings_privacy),
            stringResource(R.string.settings_privacy_text),
            actions.onOpenPrivacy,
            isFirst = true,
        )
        SettingsRow(
            stringResource(R.string.settings_source),
            stringResource(R.string.settings_source_text),
            actions.onOpenSource,
        )
        SettingsRow(
            stringResource(R.string.licences_title),
            stringResource(R.string.settings_licences_text),
            actions.onOpenLicences,
        )
        SettingsRow(stringResource(R.string.settings_version), note = null, onClick = null, value = version)
    }
}

/** The look as a person reads it: "Follow phone", "Daylight" or "Night". */
@Composable
internal fun lookLabel(look: Look): String = when (look) {
    Look.FOLLOW_PHONE -> stringResource(R.string.settings_look_follow_phone)
    Look.LIGHT -> stringResource(R.string.settings_look_light)
    Look.DARK -> stringResource(R.string.settings_look_dark)
}

/** The length as a person reads it: "Off", "30 seconds" or "2 minutes". */
@Composable
internal fun minTrackLabel(seconds: Int): String = when {
    seconds <= 0 -> stringResource(R.string.settings_min_track_off)

    seconds % SECONDS_PER_MINUTE == 0 -> {
        val minutes = seconds / SECONDS_PER_MINUTE
        pluralStringResource(R.plurals.settings_minutes, minutes, minutes)
    }

    else -> pluralStringResource(R.plurals.settings_seconds, seconds, seconds)
}

@Composable
private fun scanText(state: ScanState): String = when (state) {
    ScanState.Idle -> stringResource(R.string.settings_rescan_text)

    is ScanState.Running -> stringResource(R.string.settings_rescan_running)

    is ScanState.Finished -> when (val outcome = state.outcome) {
        is ScanOutcome.Success ->
            stringResource(R.string.settings_rescan_done, outcome.summary.added, outcome.summary.removed)

        is ScanOutcome.Failure -> when (outcome.reason) {
            ScanFailure.NO_PERMISSION -> stringResource(R.string.settings_rescan_no_permission)
            ScanFailure.STORAGE_UNAVAILABLE -> stringResource(R.string.settings_rescan_no_storage)
        }
    }
}

private const val SECONDS_PER_MINUTE = 60
