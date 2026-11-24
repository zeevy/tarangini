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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            lead()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            meta?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = DigitsFont,
                    fontWeight = FontWeight.SemiBold,
                    color = look.listMuted,
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
private const val TILE_TINT = 0.12f
