/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/**
 * A framed card: the frame colour of the look, a thin edge and a soft shadow, with small corners.
 * The player card and the settings card are framed cards.
 */
@Composable
fun FrameCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(6.dp),
    spacing: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .shadow(CardShadow, shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, lookColors.frameEdge, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/**
 * The dark display, as on a hardware player. It is dark in both looks; text inside takes the
 * display text colour unless it sets its own.
 */
@Composable
fun LcdPanel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 6.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val look = lookColors
    val shape = MaterialTheme.shapes.small
    CompositionLocalProvider(LocalContentColor provides look.lcdText) {
        Column(
            modifier = modifier
                .clip(shape)
                .background(look.lcd)
                .border(1.dp, look.lcdEdge, shape)
                .padding(padding),
            content = content,
        )
    }
}

/**
 * A list on its own plain card, with a top bar: the [count] at the left (such as "252 songs") and
 * the [tools] at the right. Nothing else is around the rows, so a list never sits in a second card.
 */
@Composable
fun ListCard(
    count: String,
    modifier: Modifier = Modifier,
    tools: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val look = lookColors
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .shadow(CardShadow, shape, clip = false)
            .clip(shape)
            .background(look.list)
            .border(1.dp, look.frameEdge, shape),
    ) {
        CompositionLocalProvider(LocalContentColor provides look.listMuted) {
            Row(
                modifier = Modifier.heightIn(min = 48.dp).padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = count,
                    modifier = Modifier.weight(1f),
                    fontFamily = DigitsFont,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
                tools()
            }
        }
        HorizontalDivider(color = look.frameEdge)
        CompositionLocalProvider(LocalContentColor provides look.listText) {
            Column(Modifier.padding(4.dp), content = content)
        }
    }
}

// A soft lift from the ground, small enough to stay calm in a long list of cards.
private val CardShadow = 3.dp
