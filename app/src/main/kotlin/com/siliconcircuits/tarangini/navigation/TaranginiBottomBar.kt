/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The bottom bar: one item per tab, with [selected] marked. */
@Composable
internal fun TaranginiBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                // The label names the item, so the icon needs no description of its own.
                icon = { Icon(painter = painterResource(destination.icon), contentDescription = null) },
                label = { TabLabel(stringResource(destination.label)) },
            )
        }
    }
}

/**
 * A tab label on one line. It follows the font size the user picked, and
 * shrinks only as far as needed to fit its tab, so a long label on a narrow
 * screen is never cut or broken in the middle of a word.
 */
@Composable
private fun TabLabel(text: String, modifier: Modifier = Modifier) {
    val smallest = with(LocalDensity.current) { SmallestTabLabel.toSp() }
    val largest = LocalTextStyle.current.fontSize
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minOf(smallest.value, largest.value).sp,
            maxFontSize = largest,
            stepSize = TabLabelStep,
        ),
    )
}

// The size of the label at the standard font setting, as a fixed size that the font setting does not change.
private val SmallestTabLabel = 12.dp
private val TabLabelStep = 1.sp
