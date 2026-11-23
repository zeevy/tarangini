/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.audio.LevelReading
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.SongFormat
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.component.LevelMeter
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.PlaylistPick
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.component.songFormatText
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.PanelColor
import com.siliconcircuits.tarangini.core.ui.theme.PanelEdge
import com.siliconcircuits.tarangini.core.ui.theme.clearTopBarColors
import com.siliconcircuits.tarangini.feature.player.R
import com.siliconcircuits.tarangini.feature.player.equalizer.NowPlayingEqualizer
import com.siliconcircuits.tarangini.feature.player.lyrics.LyricsViewModel
import com.siliconcircuits.tarangini.feature.player.queue.QueueViewModel
import com.siliconcircuits.tarangini.feature.player.queue.upNext

/** The now playing screen: one card with the song, the level meter, the seek bar and the keys of the player. */
@Composable
internal fun NowPlayingScreen(
    onBackClick: (() -> Unit)?,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenEqualizer: () -> Unit = {},
    viewModel: NowPlayingViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    levelsViewModel: LevelsViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
) {
    val queue by queueViewModel.state.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lyrics by lyricsViewModel.state.collectAsStateWithLifecycle()
    // Collected here, but its value is read only inside the meter.
    val levelState = levelsViewModel.levels.collectAsStateWithLifecycle()
    val levels = remember(levelState) { { levelState.value } }
    val actions = NowPlayingActions(
        onTogglePlay = viewModel::togglePlay,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSeek = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeat,
        onToggleFavourite = viewModel::toggleFavourite,
    )
    NowPlayingContent(
        state,
        actions,
        onBackClick,
        onOpenQueue,
        modifier,
        viewModel.playlistPick,
        viewModel.options.copy(onOpenEqualizer = onOpenEqualizer),
        lyrics,
        levels = levels,
        below = {
            item(key = EQUALIZER_KEY) {
                NowPlayingEqualizer(
                    Modifier.widthIn(max = WidestCard).fillMaxWidth().padding(bottom = CardEdge),
                )
            }
            upNext(queue, WidestCard, queueViewModel::playAt, onOpenQueue)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingContent(
    state: NowPlayingUiState?,
    actions: NowPlayingActions,
    onBackClick: (() -> Unit)?,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    playlists: PlaylistPick? = null,
    options: PlaybackOptionActions = PlaybackOptionActions(),
    lyrics: String? = null,
    levels: () -> LevelReading = { LevelReading.SILENCE },
    below: LazyListScope.() -> Unit = {},
) {
    Scaffold(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = clearTopBarColors(),
                title = { TopBarTitle(state?.album) },
                // As a pane beside the tabs there is nothing to go back to.
                navigationIcon = { onBackClick?.let { BackButton(onClick = it) } },
                actions = {
                    IconButton(onClick = onOpenQueue) {
                        Icon(
                            painter = painterResource(CoreUiR.drawable.ic_queue),
                            contentDescription = stringResource(R.string.queue_open),
                        )
                    }
                    state?.let { now -> PlaybackOptionsMenu(now, options, playlists, lyrics) }
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
            // One list for the whole screen: a short screen with a large font scrolls instead of
            // cutting the keys off, and the songs of a long queue are drawn only when they come into view.
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = ScreenEdge, end = ScreenEdge, bottom = CardEdge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // On a wide screen the card keeps the width of a phone, so the keys stay in reach.
                item(key = CARD_KEY) {
                    NowPlayingCard(
                        state,
                        actions,
                        levels,
                        Modifier.widthIn(max = WidestCard).fillMaxWidth().padding(bottom = CardEdge),
                    )
                }
                // The equalizer and the next songs.
                below()
            }
        }
    }
}

/** "Now playing" with the album of the song under it. */
@Composable
private fun TopBarTitle(album: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.now_playing_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        album?.let {
            Text(text = it, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NowPlayingCard(
    state: NowPlayingUiState,
    actions: NowPlayingActions,
    levels: () -> LevelReading,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier.background(PanelColor, shape).border(PanelEdge, shape).padding(CardEdge),
        verticalArrangement = Arrangement.spacedBy(Spacing8),
    ) {
        FormatLine(state.format, state.place, state.queueSize)
        Row(horizontalArrangement = Arrangement.spacedBy(CardEdge), verticalAlignment = Alignment.Bottom) {
            // The meter stands still in a pause, while the player waits for data and after an error.
            val moving = state.showPause && !state.isBuffering && state.error == null
            LevelMeter(levels, moving)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing6)) {
                BigTime(state)
                SongTexts(state, actions.onToggleFavourite)
            }
        }
        when {
            state.error != null -> ErrorLine(state.error, actions.onTogglePlay)
            state.isLive -> Unit
            else -> SeekBar(state, actions.onSeek)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PlayerButtons(state, actions)
    }
}

/** The format of the song and its place in the queue, such as "MP3 320 kbps · 44.1 kHz · Stereo" and "2 / 8". */
@Composable
private fun FormatLine(format: SongFormat?, place: Int, queueSize: Int) {
    val rest = format?.let { songFormatText(it.copy(codec = null)) }.orEmpty()
    // One item has no place worth naming.
    val hasPlace = queueSize > 1
    if (format?.codec == null && rest.isEmpty() && !hasPlace) return
    val small = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing8)) {
        format?.codec?.let { codec ->
            Text(
                text = codec,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                style = small,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        // Before the player has read the file there is no format: an empty text would be an item with
        // nothing for a screen reader to say, so only the space is kept.
        if (rest.isEmpty()) {
            Spacer(Modifier.weight(1f))
        } else {
            Text(
                text = rest,
                modifier = Modifier.weight(1f),
                style = small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasPlace) {
            val spoken = stringResource(R.string.now_playing_place_spoken, place, queueSize)
            Text(
                text = stringResource(R.string.now_playing_place, place, queueSize),
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                style = small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The time played in large digits, with the length after it. A live stream says that it is live. */
@Composable
private fun BigTime(state: NowPlayingUiState) {
    val accent = MaterialTheme.colorScheme.primary
    val length = state.durationMs
    if (length == null) {
        // After an error the line under it says why; "Live" would say the opposite.
        if (state.error != null) return
        Text(
            text = stringResource(if (state.isBuffering) R.string.player_loading else R.string.now_playing_live),
            style = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.Bold, fontSize = 28.sp),
            color = accent,
        )
        return
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing8)) {
        // With a large font the digits get smaller to fit, before anything is cut.
        BasicText(
            text = formatDuration(state.positionMs).ifEmpty { ZERO },
            modifier = Modifier.weight(1f, fill = false),
            style = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = accent),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 38.sp),
        )
        Text(
            text = stringResource(R.string.now_playing_length, formatDuration(length).ifEmpty { ZERO }),
            modifier = Modifier.padding(bottom = 4.dp),
            style = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** The title and the artist, with the heart beside them for a song of the library. */
@Composable
private fun SongTexts(state: NowPlayingUiState, onToggleFavourite: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title ?: stringResource(CoreUiR.string.unknown_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // A station that sends no song title has no line here: the large text says that it is live.
            val hasArtistLine = state.artist != null || !state.isLive
            if (hasArtistLine) {
                Text(
                    text = state.artist ?: stringResource(CoreUiR.string.unknown_artist),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        state.isFavourite?.let { on -> FavouriteButton(on, onToggleFavourite) }
    }
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
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.player_try_again)) }
    }
}

/** The heart. Its name for a screen reader is what a tap does. */
@Composable
private fun FavouriteButton(on: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(if (on) CoreUiR.drawable.ic_favourite_filled else CoreUiR.drawable.ic_favourite),
            contentDescription = stringResource(
                if (on) CoreUiR.string.song_remove_favourite else CoreUiR.string.song_add_favourite,
            ),
            tint = if (on) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
        )
    }
}

// The space at the sides of the screen and inside the card. With 12 dp each, the five keys stay
// 48 dp wide on a phone of 320 dp.
private val ScreenEdge = 12.dp
private val CardEdge = 12.dp
private val Spacing6 = 6.dp
private val Spacing8 = 8.dp
private val WidestCard = 560.dp
private const val CARD_KEY = "card"
private const val EQUALIZER_KEY = "equalizer"

// The shared format leaves a zero out, which is right for a length that is not known. A place is never unknown.
internal const val ZERO = "0:00"
