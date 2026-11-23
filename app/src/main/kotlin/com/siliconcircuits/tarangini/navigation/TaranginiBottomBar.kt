/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.siliconcircuits.tarangini.core.ui.theme.lookColors

/** The bottom bar: a flat bar on the frame colour, one item per tab, with [selected] marked. */
@Composable
internal fun TaranginiBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        HorizontalDivider(color = lookColors.frameEdge)
        Row(Modifier.navigationBarsPadding().padding(vertical = 2.dp).selectableGroup()) {
            TopLevelDestination.entries.forEach { destination ->
                TabItem(destination, destination == selected, onSelect, Modifier.weight(1f))
            }
        }
    }
}
