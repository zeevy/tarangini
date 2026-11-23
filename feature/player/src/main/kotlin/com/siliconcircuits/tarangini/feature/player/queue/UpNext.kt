/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The next songs, as part of the list of the now playing screen: a title row with the songs left
 * and Edit, then the song that plays and every song after it. Songs already played are left out,
 * so the list starts at the song that plays; the queue screen still shows them. A long queue is
 * drawn lazily with the rest of the screen. Nothing is added for a stream or an empty queue.
 *
 * @param maxWidth the widest a row may be, the width of the card above it
 */
internal fun LazyListScope.upNext(state: QueueUiState, maxWidth: Dp, onPlay: (Int) -> Unit, onEdit: () -> Unit) {
    val current = state.currentIndex ?: return
    if (state.items.getOrNull(current)?.isStream != false) return
    val left = state.items.size - current - 1
    item(key = TITLE_KEY) { UpNextTitle(left, onEdit, Modifier.widthIn(max = maxWidth).fillMaxWidth()) }
    // Keyed by place: the same song can be in the queue twice.
    items(count = left + 1, key = { current + it }) { offset ->
        val index = current + offset
        UpNextRow(index, state.items[index], index == current, { onPlay(index) }, Modifier.widthIn(max = maxWidth))
    }
    if (left == 0) {
        item(key = END_KEY) {
            Text(
                text = stringResource(R.string.up_next_end),
                modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth().padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpNextTitle(left: Int, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.up_next_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = pluralStringResource(R.plurals.up_next_left, left, left),
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Moves and removes are on the queue screen.
        TextButton(onClick = onEdit) { Text(stringResource(R.string.up_next_edit)) }
    }
}

/**
 * One song: its place in the queue, title and artist on one line, and its length. The song that
 * plays is in the accent colour.
 */
@Composable
private fun UpNextRow(
    index: Int,
    item: PlaybackItem,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (isCurrent) colors.secondary else colors.onSurface
    val muted = if (isCurrent) colors.secondary else colors.onSurfaceVariant
    val title = item.title ?: stringResource(R.string.up_next_no_title)
    val length = item.durationMs?.let(::formatDuration)
    val spoken = listOfNotNull(
        stringResource(R.string.up_next_place, index + 1),
        title,
        item.artist,
        length,
        stringResource(R.string.up_next_playing).takeIf { isCurrent },
    ).joinToString(", ")
    val playLabel = stringResource(R.string.up_next_play)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = RowHeight)
            .clickable(onClickLabel = playLabel, onClick = onPlay)
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val digits = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(text = (index + 1).toString(), modifier = Modifier.widthIn(min = 24.dp), style = digits, color = muted)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = tint)) { append(title) }
                item.artist?.let { artist -> withStyle(SpanStyle(color = muted)) { append(" · ", artist) } }
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        length?.let { Text(text = it, style = digits, color = muted) }
    }
}

private val RowHeight = 48.dp
private const val TITLE_KEY = "up_next_title"
private const val END_KEY = "up_next_end"
