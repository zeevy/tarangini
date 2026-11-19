/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.component.PlaceholderScreen
import com.siliconcircuits.tarangini.feature.library.R
import com.siliconcircuits.tarangini.feature.library.permission.AudioPermissionGate

@Composable
internal fun LibraryScreen(modifier: Modifier = Modifier) {
    AudioPermissionGate(modifier = modifier) {
        PlaceholderScreen(title = stringResource(R.string.library_title))
    }
}
