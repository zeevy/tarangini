/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.ui.theme.GlassEdge
import com.siliconcircuits.tarangini.core.ui.theme.GlassPanel
import com.siliconcircuits.tarangini.core.ui.theme.Spacing

/**
 * The tabs on a wide window: a glass rail at the left edge in place of the bottom bar, so the
 * screens keep their full height.
 */
@Composable
internal fun TaranginiNavRail(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight().systemBarsPadding().padding(Spacing.small),
        shape = RoundedCornerShape(Spacing.large),
        color = GlassPanel,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = GlassEdge,
    ) {
        NavigationRail(containerColor = Color.Transparent, windowInsets = WindowInsets()) {
            // The tabs sit in the middle of the rail, where a thumb reaches them on a tablet held in two hands.
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationRailItem(
                        selected = destination == selected,
                        onClick = { onSelect(destination) },
                        // The label names the item, so the icon needs no description of its own.
                        icon = { Icon(painter = painterResource(destination.icon), contentDescription = null) },
                        label = { Text(stringResource(destination.label), maxLines = 1) },
                    )
                }
            }
        }
    }
}
