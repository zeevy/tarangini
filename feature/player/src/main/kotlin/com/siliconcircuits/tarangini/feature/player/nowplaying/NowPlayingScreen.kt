/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumArt
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.PlaylistPick
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.core.ui.theme.glassBackground
import com.siliconcircuits.tarangini.core.ui.theme.glassTopBarColors
import com.siliconcircuits.tarangini.feature.player.R

/** The now playing screen: the cover, the texts, the seek bar and the buttons of the player. */
@Composable
internal fun NowPlayingScreen(
    onBackClick: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = NowPlayingActions(
        onTogglePlay = viewModel::togglePlay,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSeek = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeat,
        onToggleFavourite = viewModel::toggleFavourite,
    )
    NowPlayingContent(state, actions, onBackClick, onOpenQueue, modifier, viewModel.playlistPick, viewModel.options)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingContent(
    state: NowPlayingUiState?,
    actions: NowPlayingActions,
    onBackClick: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    playlists: PlaylistPick? = null,
    options: PlaybackOptionActions = PlaybackOptionActions(),
) {
    Scaffold(
        modifier = modifier.glassBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = glassTopBarColors(),
                title = { Text(stringResource(R.string.now_playing_title)) },
                navigationIcon = { BackButton(onClick = onBackClick) },
                actions = {
                    state?.isFavourite?.let { on -> FavouriteButton(on, actions.onToggleFavourite) }
                    IconButton(onClick = onOpenQueue) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_queue),
                            contentDescription = stringResource(R.string.queue_open),
                        )
                    }
                    state?.let { now -> PlaybackOptionsMenu(now, options, playlists) }
                },
            )
        },
    ) { padding ->
        if (state == null) {
            MessageState(
                title = stringResource(R.string.now_playing_empty_title),
                text = stringResource(R.string.now_playing_empty_text),
                modifier = Modifier.padding(padding),
                icon = painterResource(CoreUiR.drawable.ic_music_note),
            )
        } else {
            Player(state, actions, Modifier.padding(padding))
        }
    }
}

/** The cover above the controls on an upright screen, and beside them on a wide one. */
@Composable
private fun Player(state: NowPlayingUiState, actions: NowPlayingActions, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(horizontal = Spacing.large)) {
        // Read here: inside the row and the column the names would mean something else.
        val width = maxWidth
        val height = maxHeight
        if (width > height) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(state.artworkUri, size = coverSide(height - Spacing.large, width / 2))
                // A wide screen is low: with one line for the title every button fits without a scroll.
                Controls(state, actions, Modifier.weight(1f).verticalScroll(rememberScrollState()), titleLines = 1)
            }
        } else {
            Column(
                // A short screen with a large font scrolls instead of cutting the buttons off.
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.large, Alignment.CenterVertically),
            ) {
                AlbumArt(state.artworkUri, size = coverSide(width, height / 2))
                Controls(state, actions)
            }
        }
    }
}

/** The side of the cover: as large as the space allows, but never larger than a hand can take in. */
private fun coverSide(first: Dp, second: Dp): Dp = min(min(first, second), Spacing.extraLarge * LARGEST_COVER)

@Composable
private fun Controls(
    state: NowPlayingUiState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
    titleLines: Int = 2,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = state.title ?: stringResource(CoreUiR.string.unknown_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis,
        )
        // A station that sends no song title has no line here. The line below says that it is live.
        val hasArtistLine = state.artist != null || !state.isLive
        if (hasArtistLine) OneLine(state.artist ?: stringResource(CoreUiR.string.unknown_artist))
        // A station has no album. An empty line would only push the buttons down.
        state.album?.let { OneLine(it) }
        when {
            state.error != null -> ErrorLine(state.error, actions.onTogglePlay)
            state.isLive -> LiveLine(state.isBuffering)
            else -> SeekBar(state, actions.onSeek)
        }
        PlayerButtons(state, actions)
    }
}

@Composable
private fun OneLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Why the item does not play, with a button that tries again. It takes the place of the seek bar. */
@Composable
private fun ErrorLine(error: PlaybackError, onRetry: () -> Unit) {
    val text = when (error) {
        PlaybackError.NETWORK -> R.string.player_error_network
        PlaybackError.FILE_NOT_AVAILABLE -> R.string.player_error_file
        PlaybackError.CANNOT_PLAY -> R.string.player_error_cannot_play
        PlaybackError.UNKNOWN -> R.string.player_error_unknown
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.player_try_again)) }
    }
}

/** A live stream has no length, so there is nothing to seek in. The line says that it is live, or that it loads. */
@Composable
private fun LiveLine(isBuffering: Boolean) {
    Text(
        text = stringResource(if (isBuffering) R.string.player_loading else R.string.now_playing_live),
        modifier = Modifier.padding(vertical = Spacing.medium),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

private const val LARGEST_COVER = 7

/** The heart in the top bar. Its name for a screen reader is what a tap does. */
@Composable
private fun FavouriteButton(on: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(if (on) CoreUiR.drawable.ic_favourite_filled else CoreUiR.drawable.ic_favourite),
            contentDescription = stringResource(
                if (on) CoreUiR.string.song_remove_favourite else CoreUiR.string.song_add_favourite,
            ),
            tint = if (on) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
    }
}
