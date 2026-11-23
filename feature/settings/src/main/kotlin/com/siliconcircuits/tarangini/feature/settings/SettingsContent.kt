/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.siliconcircuits.tarangini.core.domain.library.ScanFailure
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.model.Look
import com.siliconcircuits.tarangini.core.ui.component.LedSwitch
import com.siliconcircuits.tarangini.core.ui.theme.Spacing

/** The rows of Settings in groups: Library, History, Backup (the [backup] slot) and About. */
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
        SettingsDialog.LOOK -> LookDialog(state.look, actions.onSetLook, close)

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
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsGroupTitle(stringResource(R.string.settings_look))
        SettingsRow(
            title = stringResource(R.string.settings_colours),
            text = lookLabel(state.look),
            onClick = { dialog = SettingsDialog.LOOK },
        )

        SettingsGroupTitle(stringResource(R.string.settings_sound))
        SettingsSwitchRow(
            title = stringResource(R.string.settings_levelling),
            text = stringResource(R.string.settings_levelling_text),
            on = state.levelling,
            onChange = actions.onSetLevelling,
        )

        SettingsGroupTitle(stringResource(R.string.settings_library))
        val hidden = state.scan.hiddenFolders.size
        SettingsRow(
            title = stringResource(R.string.settings_hidden_folders),
            text = if (hidden == 0) {
                stringResource(R.string.settings_hidden_folders_none)
            } else {
                pluralStringResource(R.plurals.settings_hidden_folders_count, hidden, hidden)
            },
            onClick = { dialog = SettingsDialog.FOLDERS },
        )
        SettingsRow(
            title = stringResource(R.string.settings_min_track),
            text = minTrackLabel(state.scan.minTrackSeconds),
            onClick = { dialog = SettingsDialog.MIN_TRACK },
        )
        SettingsRow(stringResource(R.string.settings_rescan), scanText(state.scanState), actions.onRescan)

        SettingsGroupTitle(stringResource(R.string.settings_history))
        SettingsRow(
            title = stringResource(R.string.settings_clear_history),
            text = stringResource(R.string.settings_clear_history_text),
            onClick = { dialog = SettingsDialog.CLEAR_HISTORY },
        )

        // The backup rows have their own state, so a test of the other rows needs none of it.
        backup()

        SettingsGroupTitle(stringResource(R.string.settings_about))
        SettingsRow(stringResource(R.string.settings_version), version, onClick = null)
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
        SettingsRow(
            stringResource(R.string.settings_privacy),
            stringResource(R.string.settings_privacy_text),
            actions.onOpenPrivacy,
        )
    }
}

@Composable
internal fun SettingsGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = Spacing.medium, end = Spacing.medium, top = Spacing.large, bottom = Spacing.small)
            .semantics { heading() },
    )
}

/** One setting: its name and its value. A row with no [onClick] only shows something. */
@Composable
internal fun SettingsRow(title: String, text: String, onClick: (() -> Unit)?) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = { Text(text) },
        modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick),
    )
}

/** A setting that is on or off. The whole row takes the tap and TalkBack hears it as a switch. */
@Composable
internal fun SettingsSwitchRow(title: String, text: String, on: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = { Text(text) },
        trailingContent = { LedSwitch(on) },
        modifier = Modifier.toggleable(value = on, role = Role.Switch, onValueChange = onChange),
    )
}

/** The look as a person reads it: "Follow the phone", "Light" or "Dark". */
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
