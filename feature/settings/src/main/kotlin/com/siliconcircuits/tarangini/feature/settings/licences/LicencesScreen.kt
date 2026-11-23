/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.licences

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.OpenSourceLibrary
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.core.ui.theme.clearTopBarColors
import com.siliconcircuits.tarangini.feature.settings.R

/** The open source licences: the notice of the app, then every library with its licence. */
@Composable
internal fun LicencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LicencesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LicencesContent(state, onOpen = viewModel::open, onClose = viewModel::close, onBack = onBack, modifier = modifier)
}

/** The list, or the licence text that is open. Back from a text returns to the list. */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still marked experimental in Material 3.
@Composable
internal fun LicencesContent(
    state: LicencesUiState,
    onOpen: (OpenSourceLibrary) -> Unit,
    onClose: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opened = state.opened
    // Kept here, so the list is at the same place after a licence text was read.
    val listState = rememberLazyListState()
    BackHandler(enabled = opened != null, onBack = onClose)
    Scaffold(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = clearTopBarColors(),
                title = {
                    Text(
                        opened?.title ?: stringResource(R.string.licences_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { BackButton(onClick = if (opened != null) onClose else onBack) },
            )
        },
    ) { innerPadding ->
        if (opened != null) {
            LicenceText(opened, Modifier.padding(innerPadding))
        } else {
            LibraryList(state.libraries.orEmpty(), listState, onOpen, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun LicenceText(opened: OpenedLicence, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium)) {
        // The licence asks that the notice of a library goes with the app, so it comes first.
        opened.notice?.let { notice ->
            Text(
                stringResource(R.string.licences_notice_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(notice, style = MaterialTheme.typography.bodySmall)
            Text(
                opened.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.large).semantics { heading() },
            )
        }
        Text(opened.text ?: stringResource(R.string.licences_no_text), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LibraryList(
    libraries: List<OpenSourceLibrary>,
    listState: LazyListState,
    onOpen: (OpenSourceLibrary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        item(key = "own") {
            Column(modifier = Modifier.padding(Spacing.medium)) {
                Text(stringResource(R.string.licences_own_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.licences_own_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Two libraries can have the same name, so the rows have no key of their own.
        items(libraries) { library ->
            ListItem(
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(library.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text(listOf(library.version, library.licence.name).filter { it.isNotEmpty() }.joinToString(" · "))
                },
                modifier = Modifier.clickable { onOpen(library) },
            )
        }
    }
}
