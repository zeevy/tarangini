/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.permission

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.R as UiR
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.theme.glassBackground
import com.siliconcircuits.tarangini.core.ui.theme.glassTopBarColors
import com.siliconcircuits.tarangini.feature.library.R

/**
 * Shown in place of the library while the app may not read audio files. It says why the app asks,
 * and has one button: the question of Android, or the app settings when Android no longer asks.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still marked experimental in Material 3.
@Composable
internal fun AudioPermissionScreen(
    state: AudioPermissionState,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.library_title),
) {
    Scaffold(
        modifier = modifier.glassBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = glassTopBarColors(),
                title = { Text(title) },
            )
        },
    ) { innerPadding ->
        val blocked = state == AudioPermissionState.DENIED_FOR_GOOD
        MessageState(
            modifier = Modifier.padding(innerPadding),
            icon = painterResource(UiR.drawable.ic_library),
            title = stringResource(
                when (state) {
                    AudioPermissionState.DENIED -> R.string.library_permission_denied_title
                    AudioPermissionState.DENIED_FOR_GOOD -> R.string.library_permission_blocked_title
                    else -> R.string.library_permission_title
                },
            ),
            text = stringResource(
                when (state) {
                    AudioPermissionState.DENIED -> R.string.library_permission_denied_text
                    AudioPermissionState.DENIED_FOR_GOOD -> R.string.library_permission_blocked_text
                    else -> R.string.library_permission_reason
                },
            ),
            actionLabel = stringResource(
                if (blocked) R.string.library_permission_open_settings else R.string.library_permission_allow,
            ),
            onAction = if (blocked) onOpenSettings else onAllow,
        )
    }
}
