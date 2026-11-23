/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/**
 * The tabs on a wide window: a flat rail at the left edge in place of the bottom bar, so the
 * screens keep their full height.
 */
@Composable
internal fun TaranginiNavRail(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)) {
        // The tabs sit in the middle of the rail, where a thumb reaches them on a tablet held in two hands.
        Column(
            modifier = Modifier.width(RailWidth).fillMaxHeight().systemBarsPadding().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                TabItem(destination, destination == selected, onSelect, Modifier.fillMaxWidth())
            }
        }
        VerticalDivider(color = lookColors.frameEdge)
    }
}

private val RailWidth = 80.dp
