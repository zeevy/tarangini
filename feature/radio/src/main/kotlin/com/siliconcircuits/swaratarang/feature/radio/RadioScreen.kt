/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.feature.radio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.swaratarang.core.ui.component.PlaceholderScreen
import com.siliconcircuits.swaratarang.feature.radio.R

@Composable
internal fun RadioScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.radio_title), modifier = modifier)
}
