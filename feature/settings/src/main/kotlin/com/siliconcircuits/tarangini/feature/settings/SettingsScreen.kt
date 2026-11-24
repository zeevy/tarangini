/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.ui.component.PlainCard
import com.siliconcircuits.tarangini.core.ui.component.ScreenHeader
import com.siliconcircuits.tarangini.feature.settings.backup.BackupSection

/**
 * The frame of Settings: the header with the way back, and one plain card for the rows. What is in
 * the card is given by the caller, so the frame can be shown where the settings store is not there,
 * as in a test of the app shell.
 */
@Composable
internal fun SettingsScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            // Settings has no bottom bar and no mini player, so it keeps clear of the gesture bar itself.
            .navigationBarsPadding()
            .padding(horizontal = 10.dp),
    ) {
        ScreenHeader(stringResource(R.string.settings_title), onBack = onBackClick)
        PlainCard(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)) { content() }
    }
}

/**
 * What Settings lists. The app puts it into the frame of Settings.
 *
 * @param onOpenLicences opens the open source licences screen
 */
@Composable
fun SettingsSections(onOpenLicences: () -> Unit, modifier: Modifier = Modifier) {
    LoadedSettings(onOpenLicences, modifier)
}

@Composable
private fun LoadedSettings(
    onOpenLicences: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val links = LocalUriHandler.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val actions = remember(viewModel, links, onOpenLicences) {
        SettingsActions(
            onSetLook = viewModel::setLook,
            onSetMinTrackSeconds = viewModel::setMinTrackSeconds,
            onSetFolderHidden = viewModel::setFolderHidden,
            onRescan = viewModel::rescan,
            onClearHistory = viewModel::clearHistory,
            onSetLevelling = viewModel::setLevelling,
            // A phone with no browser cannot open the page. The address is on the row, so nothing else is said.
            onOpenSource = { runCatching { links.openUri(SOURCE_CODE) } },
            onOpenLicences = onOpenLicences,
            onOpenPrivacy = { runCatching { links.openUri(PRIVACY) } },
        )
    }
    SettingsContent(state, version, actions, modifier, backup = { BackupSection() })
}

/** Where the source code of the app is. */
private const val SOURCE_CODE = "https://github.com/zeevy/tarangini"
private const val PRIVACY = "https://github.com/zeevy/tarangini/blob/main/docs/PRIVACY.md"
