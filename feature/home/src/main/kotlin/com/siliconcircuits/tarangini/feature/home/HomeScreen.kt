/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.PlaceholderScreen

@Composable
internal fun HomeScreen(onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.home_title),
        modifier = modifier,
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_settings),
                    contentDescription = stringResource(R.string.home_open_settings),
                )
            }
        },
    )
}
