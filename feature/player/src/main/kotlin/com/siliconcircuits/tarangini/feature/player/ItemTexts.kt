/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR

/** The title of an item with its artist under it, one line each. A text that is not known gets a stand-in. */
@Composable
internal fun ItemTexts(title: String?, artist: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title ?: stringResource(CoreUiR.string.unknown_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = artist ?: stringResource(CoreUiR.string.unknown_artist),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
