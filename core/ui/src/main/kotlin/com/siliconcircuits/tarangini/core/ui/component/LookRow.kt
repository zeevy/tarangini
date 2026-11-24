/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.R
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/**
 * One row of a list in the look of the app: a tile at the start, the title with a line under it,
 * a short value such as the length at the end, and room for a button after it. The row is one
 * button that does [onClick]; TalkBack names that with [clickLabel].
 *
 * @param lead the tile at the start, such as [AlbumArt] or [IconTile]
 * @param meta a short value in digits at the end, such as "4:05", or null
 * @param isCurrent true for the row of the song that plays: it is lit
 * @param columns more texts after the title, each in its own column, as in the songs table of a
 *   tablet; the [meta] then has a fixed width so the columns line up
 * @param end a button after the row, such as a menu, or nothing
 */
@Composable
fun LookRow(
    title: String,
    sub: String?,
    onClick: () -> Unit,
    clickLabel: String,
    lead: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    isCurrent: Boolean = false,
    columns: List<String> = emptyList(),
    end: @Composable () -> Unit = {},
) {
    val look = lookColors
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(if (isCurrent) look.rowOn else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = RowHeight)
                .clickable(onClickLabel = clickLabel, onClick = onClick)
                .padding(start = RowStart, top = 4.dp, bottom = 4.dp, end = RowEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RowGap),
        ) {
            lead()
            val titleWeight = if (columns.isEmpty()) 1f else TITLE_COLUMN
            Column(Modifier.weight(titleWeight), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isCurrent) look.accentOnList else look.listText,
                    maxLines = rowTextLines(),
                    overflow = TextOverflow.Ellipsis,
                )
                sub?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = look.listMuted,
                        maxLines = rowTextLines(),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            columns.forEach { text ->
                Text(
                    text = text,
                    modifier = Modifier.weight(OTHER_COLUMN),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = look.listMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // In a table a row with no time keeps its place, so the columns stay in line.
            if (meta == null && columns.isNotEmpty()) Spacer(Modifier.width(TimeColumn))
            meta?.let {
                Text(
                    text = it,
                    modifier = if (columns.isEmpty()) Modifier else Modifier.widthIn(min = TimeColumn),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = DigitsFont,
                    fontWeight = FontWeight.SemiBold,
                    color = look.listMuted,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
        end()
    }
}

/**
 * A small tile with an icon on the accent tint, for a row that is not a song: a genre, a folder,
 * a playlist. With [round] and no icon it shows the [initials], as for an artist.
 */
@Composable
fun IconTile(icon: Painter?, modifier: Modifier = Modifier, initials: String = "", round: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(TileSize)
            .clip(if (round) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.small)
            .background(accent.copy(alpha = TILE_TINT)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = lookColors.accentOnList)
        } else {
            Text(
                text = initials,
                color = lookColors.accentOnList,
                fontFamily = DigitsFont,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** The arrow at the end of a row that opens a list. The whole row is the button, so it says nothing. */
@Composable
fun OpenArrow(modifier: Modifier = Modifier) {
    Box(modifier.width(40.dp).heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = lookColors.listMuted,
        )
    }
}

/** Up to two first letters of the words of [name], such as "MR" for "Morning Ragas". */
fun initialsOf(name: String): String =
    name.split(' ').filter { it.firstOrNull()?.isLetter() == true }.take(2).joinToString("") { it.first().uppercase() }

/** The side of a tile at the start of a row. */
val TileSize = 40.dp

private val RowHeight = 56.dp

// The edges and the gap inside a row. The header of the songs table uses them too, to line up.
internal val RowStart = 8.dp
internal val RowEnd = 4.dp
internal val RowGap = 12.dp

/** The width of one icon button at the end of a row: the heart or the menu. */
internal val ButtonWidth = 48.dp
private const val TILE_TINT = 0.12f

// The share of the width of the title and of each other column in a table row.
internal const val TITLE_COLUMN = 2f
internal const val OTHER_COLUMN = 1.3f

/** The width of the time at the end of a table row: room for "1:02:03". */
internal val TimeColumn = 60.dp
