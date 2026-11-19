/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.component.PlaceholderScreen
import com.siliconcircuits.tarangini.feature.radio.R

@Composable
internal fun RadioScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.radio_title), modifier = modifier)
}
