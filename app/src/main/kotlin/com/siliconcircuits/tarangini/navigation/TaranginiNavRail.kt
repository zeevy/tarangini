/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.R
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/**
 * The tabs on a wide window: a flat rail at the left edge in place of the bottom bar, so the
 * screens keep their full height. The mark of the app is at the top and Settings at the bottom.
 */
@Composable
internal fun TaranginiNavRail(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)) {
        BoxWithConstraints(Modifier.width(RailWidth).fillMaxHeight().systemBarsPadding()) {
            // A low window, as a phone on its side, has no room for the mark, and with a large font
            // the tabs may not fit: then the rail scrolls.
            val low = maxHeight < LowRail
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (low) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!low) AppMark()
                // The tabs sit in the middle of the rail, where a thumb reaches them on a tablet held in two hands.
                Column(
                    modifier = (if (low) Modifier else Modifier.weight(1f)).fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        TabItem(destination, destination == selected, onSelect, Modifier.fillMaxWidth())
                    }
                }
                IconButton(onClick = onSettings) {
                    Icon(painterResource(CoreUiR.drawable.ic_settings), stringResource(R.string.rail_settings))
                }
            }
        }
        VerticalDivider(color = lookColors.frameEdge)
    }
}

/** The small dark display with four bars, the mark of the app. It is only a picture. */
@Composable
private fun AppMark() {
    val look = lookColors
    val shape = MaterialTheme.shapes.small
    Row(
        modifier = Modifier
            .size(40.dp)
            .background(look.lcd, shape)
            .border(1.dp, look.lcdEdge, shape)
            .padding(bottom = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        MarkBars.forEach { height ->
            Box(Modifier.width(4.dp).height(height).background(look.lcdAccent, RoundedCornerShape(1.dp)))
        }
    }
}

// The heights of the four bars of the mark, as in the launcher icon.
private val MarkBars = listOf(10.dp, 18.dp, 14.dp, 22.dp)

private val RailWidth = 80.dp

// Under this height the rail drops the mark and may scroll.
private val LowRail = 480.dp
