/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.feature.library.permission

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.R as UiR
import com.siliconcircuits.tarangini.core.ui.component.MessageState
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
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) },
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
