/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.radio.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.station.AddressFault
import com.siliconcircuits.tarangini.core.domain.station.NameFault
import com.siliconcircuits.tarangini.core.domain.station.StreamTest
import com.siliconcircuits.tarangini.core.domain.station.StreamTestFault
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.radio.R

/** The screen that adds or changes a station. It closes by itself when the station is saved. */
@Composable
internal fun StationFormScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StationFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The effect must call the newest way back, not the one of the first draw.
    val close by rememberUpdatedState(onBack)
    LaunchedEffect(state.isSaved) { if (state.isSaved) close() }
    val actions =
        StationFormActions(
            viewModel::onNameChange,
            viewModel::onAddressChange,
            viewModel::test,
            viewModel::save,
            onBack,
        )
    StationFormContent(state, viewModel.name, viewModel.address, actions, modifier)
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still marked experimental in Material 3.
@Composable
internal fun StationFormContent(
    state: StationFormUiState,
    name: String,
    address: String,
    actions: StationFormActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isEdit) R.string.station_form_edit else R.string.station_form_add))
                },
                navigationIcon = { BackButton(onClick = actions.onBack) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = actions.onNameChange,
                label = { Text(stringResource(R.string.station_form_name)) },
                isError = state.nameFault != null,
                supportingText = state.nameFault?.let { fault -> { Text(stringResource(fault.text())) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = address,
                onValueChange = actions.onAddressChange,
                label = { Text(stringResource(R.string.station_form_address)) },
                isError = state.addressFault != null,
                supportingText = {
                    Text(stringResource(state.addressFault?.text() ?: R.string.station_form_address_help))
                },
                singleLine = true,
                // An address is not a word: no correction, no capital letter, and the keyboard for addresses.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                OutlinedButton(onClick = actions.onTest) { Text(stringResource(R.string.station_form_test)) }
                Button(onClick = actions.onSave) { Text(stringResource(R.string.station_form_save)) }
            }
            ResultLine(state)
        }
    }
}

/** What the try of the address gave, or that the save failed. A screen reader reads it when it changes. */
@Composable
private fun ResultLine(state: StationFormUiState) {
    val test = state.test
    val text = when {
        state.saveFailed -> stringResource(R.string.station_form_save_failed)
        test is TestState.Running -> stringResource(R.string.station_test_running)
        test is TestState.Done -> test.result.text()
        else -> null
    }
    val isBad = state.saveFailed || (test is TestState.Done && test.result is StreamTest.Failed)
    if (text != null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isBad) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun StreamTest.text(): String = when (this) {
    is StreamTest.Found -> listOfNotNull(
        stringResource(R.string.station_test_found, contentType),
        stationName?.let { stringResource(R.string.station_test_name, it) },
        bitrateKbps?.let { stringResource(R.string.station_test_bitrate, it) },
    ).joinToString(" ")

    is StreamTest.Failed -> stringResource(fault.text())
}

@StringRes
private fun NameFault.text(): Int = when (this) {
    NameFault.EMPTY -> R.string.station_name_empty
    NameFault.TOO_LONG -> R.string.station_name_too_long
}

@StringRes
private fun AddressFault.text(): Int = when (this) {
    AddressFault.EMPTY -> R.string.station_address_empty
    AddressFault.TOO_LONG -> R.string.station_address_too_long
    AddressFault.ODD_CHARACTERS -> R.string.station_address_odd_characters
    AddressFault.NOT_HTTP -> R.string.station_address_not_http
    AddressFault.NO_HOST -> R.string.station_address_no_host
    AddressFault.HAS_LOGIN -> R.string.station_address_has_login
    AddressFault.ALREADY_SAVED -> R.string.station_address_already_saved
}

@StringRes
private fun StreamTestFault.text(): Int = when (this) {
    StreamTestFault.BAD_ADDRESS -> R.string.station_test_bad_address
    StreamTestFault.NO_ANSWER -> R.string.station_test_no_answer
    StreamTestFault.BAD_ANSWER -> R.string.station_test_bad_answer
    StreamTestFault.BAD_PLAYLIST -> R.string.station_test_bad_playlist
    StreamTestFault.NOT_A_STREAM -> R.string.station_test_not_a_stream
}
