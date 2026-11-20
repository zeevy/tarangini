/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.theme.glassBackground
import com.siliconcircuits.tarangini.core.ui.theme.glassTopBarColors

/**
 * The frame of Home: the name of the app and the way to Settings. What is under the bar is given
 * by the caller, so the frame can be shown where the lists of the person are not there, as in a test
 * of the app shell.
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still marked experimental in Material 3.
@Composable
internal fun HomeScreen(onSettingsClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Scaffold(
        modifier = modifier.glassBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = glassTopBarColors(),
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_settings),
                            contentDescription = stringResource(R.string.home_open_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) { content() }
    }
}

/**
 * What Home lists: the item to go on with, the songs heard last and the playlists. The app puts
 * it into the frame of Home.
 *
 * @param onOpenLibrary opens the Library tab, where the first songs are found
 * @param onOpenPlaylist opens the screen of a playlist
 */
@Composable
fun HomeSections(onOpenLibrary: () -> Unit, onOpenPlaylist: (Playlist) -> Unit, modifier: Modifier = Modifier) {
    HomeSections(HomeLinksOut(onOpenLibrary, onOpenPlaylist), modifier)
}

private class HomeLinksOut(val onOpenLibrary: () -> Unit, val onOpenPlaylist: (Playlist) -> Unit)

@Composable
private fun HomeSections(out: HomeLinksOut, modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        songActions = viewModel.songActions,
        links = HomeLinks(viewModel::resume, out.onOpenLibrary, out.onOpenPlaylist),
        modifier = modifier,
    )
}
