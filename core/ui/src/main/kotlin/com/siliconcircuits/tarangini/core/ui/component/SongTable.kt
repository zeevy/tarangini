/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.ui.R
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/**
 * The titles above the songs table of a wide screen: Title, Artist, Album and Time, over the
 * columns of [SongRow] with `inTable`. TalkBack skips them; each row says its own texts.
 */
@Composable
fun SongTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(30.dp)
            .padding(start = HeaderStart, end = HeaderEnd)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderText(stringResource(R.string.table_title), Modifier.weight(TITLE_COLUMN))
        HeaderText(stringResource(R.string.table_artist), Modifier.weight(OTHER_COLUMN))
        HeaderText(stringResource(R.string.table_album), Modifier.weight(OTHER_COLUMN))
        HeaderText(stringResource(R.string.table_time), Modifier.widthIn(min = TimeColumn), TextAlign.End)
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.9.sp,
        color = lookColors.listMuted,
        textAlign = align,
        maxLines = 1,
    )
}

// The header starts where the title of a row starts: the row edge, the tile and the gap after it.
private val HeaderStart = RowStart + TileSize + RowGap

// It ends where the time of a row ends: before the heart and the menu.
private val HeaderEnd = RowEnd + ButtonWidth * 2
