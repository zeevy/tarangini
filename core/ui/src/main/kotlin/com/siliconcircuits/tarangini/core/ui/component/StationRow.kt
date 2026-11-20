/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.ui.R

/**
 * One station in a list: its name, its stream address and a menu. A tap plays it. Every list of
 * stations uses this row.
 */
@Composable
fun StationRow(station: Station, actions: StationRowActions, modifier: Modifier = Modifier) {
    ListItem(
        colors = clearRowColors(),
        headlineContent = { OneLine(station.name) },
        supportingContent = { OneLine(station.streamUrls.firstOrNull().orEmpty()) },
        leadingContent = { IconTile(painterResource(R.drawable.ic_radio)) },
        trailingContent = { StationMenu(station, actions) },
        modifier = modifier.clickable(onClickLabel = stringResource(R.string.action_play)) { actions.onPlay(station) },
    )
}

@Composable
private fun StationMenu(station: Station, actions: StationRowActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = stringResource(R.string.station_menu, station.name),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.station_edit)) },
                onClick = {
                    open = false
                    actions.onEdit(station)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.station_remove)) },
                onClick = {
                    open = false
                    actions.onRemove(station)
                },
            )
        }
    }
}
