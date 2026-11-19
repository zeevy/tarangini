/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
