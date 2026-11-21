/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.lyrics

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The lyrics of the song that plays. The text scrolls: lyrics are longer than a screen.
 *
 * @param lyrics the text with its line breaks
 * @param title the title of the song, or null when it has none
 */
@Composable
internal fun LyricsDialog(lyrics: String, title: String?, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title ?: stringResource(R.string.now_playing_lyrics), maxLines = 2) },
        text = { Text(lyrics, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(CoreUiR.string.action_ok)) } },
    )
}
