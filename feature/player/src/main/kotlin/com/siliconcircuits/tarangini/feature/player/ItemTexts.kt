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

/**
 * The title of an item with its artist under it, [lines] lines each at most. A text that is not known gets a
 * stand-in. For a stream the second line is the station while a song title is known, else "Live".
 */
@Composable
internal fun ItemTexts(
    title: String?,
    artist: String?,
    modifier: Modifier = Modifier,
    isStream: Boolean = false,
    lines: Int = 1,
) {
    Column(modifier = modifier) {
        Text(
            text = title ?: stringResource(CoreUiR.string.unknown_title),
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            // A station that sends no song title has no second text. "Unknown artist" would be wrong for it.
            text = artist ?: stringResource(if (isStream) R.string.now_playing_live else CoreUiR.string.unknown_artist),
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
