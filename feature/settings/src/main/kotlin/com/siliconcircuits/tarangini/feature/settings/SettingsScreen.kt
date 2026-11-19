/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.feature.settings

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.PlaceholderScreen

@Composable
internal fun SettingsScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(CoreUiR.drawable.ic_back),
                    contentDescription = stringResource(CoreUiR.string.action_back),
                )
            }
        },
    )
}
