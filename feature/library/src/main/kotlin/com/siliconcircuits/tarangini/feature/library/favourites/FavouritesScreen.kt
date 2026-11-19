/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.favourites

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.component.PlaceholderScreen
import com.siliconcircuits.tarangini.feature.library.R

@Composable
internal fun FavouritesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.favourites_title), modifier = modifier)
}
