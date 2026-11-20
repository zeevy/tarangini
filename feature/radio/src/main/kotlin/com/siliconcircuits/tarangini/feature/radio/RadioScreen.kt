/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Station
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.StationRow
import com.siliconcircuits.tarangini.core.ui.component.StationRowActions

/** The Radio tab. It needs no permission: a stream is not a file of the phone. */
@Composable
internal fun RadioScreen(
    onAdd: () -> Unit,
    onEdit: (Station) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RadioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RadioContent(state, RadioActions(viewModel::play, onAdd, onEdit, viewModel::remove), modifier)
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still marked experimental in Material 3.
@Composable
internal fun RadioContent(state: RadioUiState, actions: RadioActions, modifier: Modifier = Modifier) {
    // The station the person is asked about, while the question is open.
    var removing by remember { mutableStateOf<Station?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.radio_title)) },
                actions = {
                    IconButton(onClick = actions.onAdd) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_add),
                            contentDescription = stringResource(R.string.radio_add_station),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val body = Modifier.fillMaxSize().padding(innerPadding)
        val stations = state.stations
        when {
            state.failed && stations.isNullOrEmpty() -> MessageState(
                title = stringResource(R.string.radio_failed_title),
                text = stringResource(R.string.radio_failed_text),
                modifier = body,
            )

            // Nothing is drawn for the moment the first read takes.
            stations == null -> Unit

            stations.isEmpty() -> MessageState(
                title = stringResource(R.string.radio_empty_title),
                text = stringResource(R.string.radio_empty_text),
                modifier = body,
                icon = painterResource(CoreUiR.drawable.ic_radio),
                actionLabel = stringResource(R.string.radio_add_station),
                onAction = actions.onAdd,
            )

            else -> {
                val rowActions = StationRowActions(actions.onPlay, actions.onEdit, onRemove = { removing = it })
                LazyColumn(modifier = body) {
                    items(stations, key = { it.id }) { station -> StationRow(station, rowActions) }
                }
            }
        }
    }
    removing?.let { station ->
        RemoveQuestion(
            station = station,
            onConfirm = {
                removing = null
                actions.onRemove(station)
            },
            onDismiss = { removing = null },
        )
    }
}

/** Asks before a station goes: a removed station cannot be brought back. */
@Composable
private fun RemoveQuestion(station: Station, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.radio_remove_title, station.name)) },
        text = { Text(stringResource(R.string.radio_remove_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.radio_remove_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.radio_remove_cancel)) } },
    )
}
