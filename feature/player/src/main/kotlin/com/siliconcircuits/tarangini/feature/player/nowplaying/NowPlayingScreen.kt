/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.FrameCard
import com.siliconcircuits.tarangini.core.ui.component.LcdPanel
import com.siliconcircuits.tarangini.core.ui.component.PlaylistNameDialog
import com.siliconcircuits.tarangini.core.ui.component.PlaylistPick
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.player.R
import com.siliconcircuits.tarangini.feature.player.lyrics.LyricsViewModel
import com.siliconcircuits.tarangini.feature.player.queue.PendingRemoval
import com.siliconcircuits.tarangini.feature.player.queue.QueueActions
import com.siliconcircuits.tarangini.feature.player.queue.QueueNotice
import com.siliconcircuits.tarangini.feature.player.queue.QueueUiState
import com.siliconcircuits.tarangini.feature.player.queue.QueueViewModel
import com.siliconcircuits.tarangini.feature.player.queue.UpNextCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/**
 * Where the player leads to in other parts of the app.
 *
 * @property onOpenAlbum opens an album by its id, title and artist
 */
data class PlayerTargets(
    val onOpenSearch: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenLibrary: () -> Unit = {},
    val onOpenAlbum: (id: Long, title: String, artist: String) -> Unit = { _, _, _ -> },
)

/** The player tab: the display card, the seek row and Up next. */
@Composable
internal fun NowPlayingScreen(
    targets: PlayerTargets,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    visualViewModel: VisualViewModel = hiltViewModel(),
    queueViewModel: QueueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val queue by queueViewModel.state.collectAsStateWithLifecycle()
    val removal by queueViewModel.pendingRemoval.collectAsStateWithLifecycle()
    val lyrics by lyricsViewModel.state.collectAsStateWithLifecycle()
    val mode by visualViewModel.mode.collectAsStateWithLifecycle()
    val levelling by visualViewModel.levelling.collectAsStateWithLifecycle()
    // Collected here, but its value is read only while the visual draws.
    val shownState = visualViewModel.shown.collectAsStateWithLifecycle()
    val moving = state?.let { it.showPause && !it.isBuffering && it.error == null } == true
    LaunchedEffect(moving) { visualViewModel.setMoving(moving) }
    NowPlayingContent(
        state = state,
        actions = NowPlayingActions(
            onTogglePlay = viewModel::togglePlay,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onSeek = viewModel::seekTo,
            onSeekBy = viewModel::seekBy,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeat,
            onToggleFavourite = viewModel::toggleFavourite,
        ),
        targets = targets,
        modifier = modifier,
        parts = DisplayParts(mode, levelling, remember(shownState) { { shownState.value } }, visualViewModel::cycle),
        queue = queue,
        queueActions = QueueActions(
            onPlay = queueViewModel::playAt,
            onMove = queueViewModel::move,
            onRemove = queueViewModel::remove,
            onClear = queueViewModel::clear,
        ),
        player = PlayerExtras(
            playlists = viewModel.playlistPick,
            options = viewModel.options.copy(onOpenSettings = targets.onOpenSettings),
            lyrics = lyrics,
            removal = removal,
            onUndoRemove = queueViewModel::undoRemove,
            onSaveQueue = queueViewModel::saveAsPlaylist,
            notices = queueViewModel.notice,
            onShuffleAll = queueViewModel::shuffleAll,
        ),
    )
}

/** The parts of the player that come with their own view models. */
internal data class PlayerExtras(
    val playlists: PlaylistPick? = null,
    val options: PlaybackOptionActions = PlaybackOptionActions(),
    val lyrics: String? = null,
    val removal: PendingRemoval? = null,
    val onUndoRemove: () -> Unit = {},
    val onSaveQueue: (String) -> Unit = {},
    val notices: Flow<QueueNotice> = emptyFlow(),
    val onShuffleAll: () -> Unit = {},
)

/** The player as it is drawn, with no view model, so tests can show every state. */
@Composable
internal fun NowPlayingContent(
    state: NowPlayingUiState?,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
    targets: PlayerTargets = PlayerTargets(),
    parts: DisplayParts = DisplayParts(),
    queue: QueueUiState = QueueUiState(),
    queueActions: QueueActions = QueueActions({}, { _, _ -> }, {}),
    player: PlayerExtras = PlayerExtras(),
) {
    val snackbar = remember { SnackbarHostState() }
    var saving by rememberSaveable { mutableStateOf(false) }
    QueueMessages(player, snackbar)
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            PlayerColumn(
                state,
                actions,
                targets,
                parts,
                PlayerQueue(
                    queue,
                    queueActions.copy(onSave = {
                        saving = true
                    }),
                ),
                player,
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(ScreenEdge)) { data ->
            val look = lookColors
            Snackbar(data, containerColor = look.snack, contentColor = look.onSnack, actionColor = look.snackAction)
        }
    }
    if (saving) {
        PlaylistNameDialog(
            title = stringResource(R.string.up_next_save),
            confirmLabel = stringResource(R.string.up_next_save_confirm),
            onConfirm = {
                saving = false
                player.onSaveQueue(it)
            },
            onDismiss = { saving = false },
        )
    }
}

/** Up next and what can be done with it. */
private class PlayerQueue(val state: QueueUiState, val actions: QueueActions)

/** The header, the whole card or the compact one, and Up next under them. */
@Composable
private fun PlayerColumn(
    state: NowPlayingUiState?,
    actions: NowPlayingActions,
    targets: PlayerTargets,
    parts: DisplayParts,
    queue: PlayerQueue,
    player: PlayerExtras,
) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var fullPx by remember { mutableIntStateOf(0) }
    var compactPx by remember { mutableIntStateOf(with(density) { CompactGuess.roundToPx() }) }
    val room by rememberUpdatedState(fullPx - compactPx)
    // While Up next is scrolled, the card shrinks to one line so more songs show. The list then
    // grows by the room the card gave up, so it shrinks only when the list still scrolls after
    // that; else it would flip back and forth.
    var compact by remember { mutableStateOf(false) }
    LaunchedEffect(list) {
        snapshotFlow { list.isScrolled() to list.scrollRange() }.collect { (scrolled, range) ->
            compact = if (compact) scrolled else scrolled && range > room
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        // The card never takes more than most of the height, so Up next always shows a few rows.
        val cardMax = maxHeight * CARD_SHARE
        Column(
            modifier = Modifier.fillMaxSize().padding(start = ScreenEdge, end = ScreenEdge, bottom = ScreenEdge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val width = Modifier.widthIn(max = WidestCard).fillMaxWidth()
            PlayerHeader(state, player, targets.onOpenSearch, width)
            if (compact && state != null) {
                CompactCard(
                    state,
                    actions,
                    parts.shown,
                    { scope.launch { list.animateScrollToItem(0) } },
                    width.onSizeChanged { compactPx = it.height },
                )
            } else {
                PlayerCard(
                    state,
                    actions,
                    parts,
                    targets,
                    player.onShuffleAll,
                    width.heightIn(max = cardMax).animateContentSize().onSizeChanged { fullPx = it.height },
                )
            }
            UpNextCard(queue.state, queue.actions, list, width.weight(1f), hidden = player.removal?.index)
        }
    }
}

/** Removed with Undo, and what Save as a playlist did. */
@Composable
private fun QueueMessages(player: PlayerExtras, snackbar: SnackbarHostState) {
    val removedText = player.removal?.let {
        stringResource(
            R.string.up_next_removed,
            it.title ?: stringResource(R.string.up_next_no_title),
        )
    }
    val undo = stringResource(R.string.up_next_undo)
    LaunchedEffect(player.removal) {
        if (removedText != null) {
            val result = snackbar.showSnackbar(removedText, actionLabel = undo, withDismissAction = false)
            if (result == SnackbarResult.ActionPerformed) player.onUndoRemove()
        }
    }
    val saved = stringResource(R.string.up_next_saved, "%s")
    val notSaved = stringResource(R.string.up_next_not_saved)
    LaunchedEffect(player.notices) {
        player.notices.collect { notice ->
            snackbar.showSnackbar(
                when (notice) {
                    is QueueNotice.Saved -> saved.replace("%s", notice.name)
                    QueueNotice.NotSaved -> notSaved
                },
            )
        }
    }
}

/** "Tarangini", where the queue came from, Search and More. */
@Composable
private fun PlayerHeader(
    state: NowPlayingUiState?,
    player: PlayerExtras,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.heightIn(min = 48.dp).padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.player_header),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = fromText(state),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onSearch) {
            Icon(painterResource(CoreUiR.drawable.ic_search), stringResource(CoreUiR.string.action_search))
        }
        PlaybackOptionsMenu(state, player.options, player.playlists, player.lyrics)
    }
}

/** "Playing from album" and the name of the list the queue came from, or "Now playing". */
@Composable
private fun fromText(state: NowPlayingUiState?): String {
    val source: QueueSource? = state?.source
    return when {
        state == null -> stringResource(R.string.player_nothing)

        source == null -> stringResource(R.string.now_playing_title)

        else -> {
            val name = if (source.kind == QueueSourceKind.FAVOURITES) {
                stringResource(R.string.source_favourites_name)
            } else {
                source.name
            }
            stringResource(R.string.player_from, stringResource(source.kind.label()), name).trim()
        }
    }
}

/** The words before the name of the list a queue came from. */
private fun QueueSourceKind.label(): Int = when (this) {
    QueueSourceKind.ALBUM -> R.string.source_album
    QueueSourceKind.ARTIST -> R.string.source_artist
    QueueSourceKind.PLAYLIST -> R.string.source_playlist
    QueueSourceKind.GENRE -> R.string.source_genre
    QueueSourceKind.FOLDER -> R.string.source_folder
    QueueSourceKind.FAVOURITES -> R.string.source_favourites
}

/**
 * The framed card: the display, and under it the seek row, or with nothing loaded the two ways
 * to start. A large font can make it taller than its room: then it scrolls inside.
 */
@Composable
private fun PlayerCard(
    state: NowPlayingUiState?,
    actions: NowPlayingActions,
    parts: DisplayParts,
    targets: PlayerTargets,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openAlbum = state?.albumId?.let { id ->
        state.album?.takeIf {
            it.isNotBlank()
        }?.let { album -> { targets.onOpenAlbum(id, album, state.artist.orEmpty()) } }
    }
    FrameCard(modifier = modifier, padding = PaddingValues(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            LcdPanel(Modifier.fillMaxWidth()) { PlayerDisplay(state, actions, parts, openAlbum) }
            if (state == null) StartButtons(onShuffleAll, targets.onOpenLibrary) else SeekRow(state, actions)
        }
    }
}

/** With nothing loaded: Shuffle all songs, or open the library to pick. */
@Composable
private fun StartButtons(onShuffleAll: () -> Unit, onOpenLibrary: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onShuffleAll,
            modifier = Modifier.weight(START_WEIGHT).heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Icon(painterResource(CoreUiR.drawable.ic_shuffle), null, Modifier.size(18.dp))
            Text(
                stringResource(R.string.player_shuffle_all),
                Modifier.padding(start = 8.dp),
                fontWeight = FontWeight.ExtraBold,
            )
        }
        OutlinedButton(
            onClick = onOpenLibrary,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text(stringResource(R.string.player_open_library), fontWeight = FontWeight.ExtraBold)
        }
    }
}

// The sides of the screen. With 8 dp the keys and the chips fit on a phone of 320 dp.
private val ScreenEdge = 8.dp
private val WidestCard = 560.dp
private const val CARD_SHARE = 0.72f
private const val START_WEIGHT = 1.4f

// The shared format leaves a zero out, which is right for a length that is not known. A place is never unknown.
internal const val ZERO = "0:00"

// The height of the compact card before it is first measured.
private val CompactGuess = 64.dp
