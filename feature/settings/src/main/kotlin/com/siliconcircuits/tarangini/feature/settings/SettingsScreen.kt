/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.theme.clearTopBarColors
import com.siliconcircuits.tarangini.feature.settings.backup.BackupSection

/**
 * The frame of Settings: the title and the way back. What is under the bar is given by the
 * caller, so the frame can be shown where the settings store is not there, as in a test of the app
 * shell.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still marked experimental in Material 3.
@Composable
internal fun SettingsScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Scaffold(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = clearTopBarColors(),
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { BackButton(onClick = onBackClick) },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) { content() }
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
