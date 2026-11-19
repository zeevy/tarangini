/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.feature.home

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.swaratarang.core.ui.R as CoreUiR
import com.siliconcircuits.swaratarang.core.ui.component.PlaceholderScreen

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
