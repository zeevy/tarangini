/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One tab of the bottom bar or the rail: the icon on a small pill when the tab is [selected],
 * and the name under it. The whole item takes the tap.
 */
@Composable
internal fun TabItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colour = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .selectable(selected = selected, role = Role.Tab) { onSelect(destination) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 30.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = PILL_ALPHA) else Color.Transparent,
                    MaterialTheme.shapes.small,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // The label names the item, so the icon needs no description of its own.
            Icon(painterResource(destination.icon), contentDescription = null, tint = colour)
        }
        TabLabel(stringResource(destination.label), colour)
    }
}

/**
 * A tab label on one line. It follows the font size the user picked, and
 * shrinks only as far as needed to fit its tab, so a long label on a narrow
 * screen is never cut or broken in the middle of a word.
 */
@Composable
private fun TabLabel(text: String, colour: Color, modifier: Modifier = Modifier) {
    val smallest = with(LocalDensity.current) { SmallestTabLabel.toSp() }
    val largest = MaterialTheme.typography.labelMedium.fontSize
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 4.dp),
        color = colour,
        style = LocalTextStyle.current.merge(MaterialTheme.typography.labelMedium),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minOf(smallest.value, largest.value).sp,
            maxFontSize = largest,
            stepSize = TabLabelStep,
        ),
    )
}

private const val PILL_ALPHA = 0.12f

// The size of the label at the standard font setting, as a fixed size that the font setting does not change.
private val SmallestTabLabel = 12.dp
private val TabLabelStep = 1.sp
