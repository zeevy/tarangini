/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.theme.glassBackground
import com.siliconcircuits.tarangini.core.ui.theme.glassTopBarColors
import com.siliconcircuits.tarangini.feature.player.R

/** The queue: the items that play, in their order. A row is dragged to another place or swiped away. */
@Composable
internal fun QueueScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    QueueContent(state, QueueActions(viewModel::playAt, viewModel::move, viewModel::remove), onBackClick, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QueueContent(
    state: QueueUiState,
    actions: QueueActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.glassBackground(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = glassTopBarColors(),
                title = { Text(stringResource(R.string.queue_title)) },
                navigationIcon = { BackButton(onClick = onBackClick) },
            )
        },
    ) { padding ->
        if (state.items.isEmpty()) {
            MessageState(
                title = stringResource(R.string.queue_empty_title),
                text = stringResource(R.string.queue_empty_text),
                modifier = Modifier.padding(padding),
                icon = painterResource(CoreUiR.drawable.ic_queue),
            )
        } else {
            QueueList(state, actions, Modifier.padding(padding))
        }
    }
}

/** One row of the list with the key it keeps while it is moved. */
private data class Keyed(val key: String, val item: PlaybackItem)

/**
 * A song can be in the queue more than once, so its id alone is no key. The key is the id with
 * the count of the same id before it.
 */
private fun List<PlaybackItem>.keyed(): List<Keyed> {
    val seen = HashMap<String, Int>()
    return map { item ->
        val count = seen.merge(item.id, 1, Int::plus) ?: 1
        Keyed("${item.id}#$count", item)
    }
}

@Composable
private fun QueueList(state: QueueUiState, actions: QueueActions, modifier: Modifier = Modifier) {
    // The order on screen. A drag changes it at once, and the player is told when the finger lifts.
    var rows by remember(state.items) { mutableStateOf(state.items.keyed()) }
    val currentKey = remember(state.items, state.currentIndex) { state.currentIndex?.let { rows.getOrNull(it)?.key } }
    // The list opens at the item that plays.
    val list = rememberLazyListState(initialFirstVisibleItemIndex = state.currentIndex ?: 0)
    var dragStart by remember { mutableIntStateOf(0) }
    var dragItems by remember { mutableStateOf<List<PlaybackItem>?>(null) }
    val onMove by rememberUpdatedState(actions.onMove)
    // The drag state lives longer than one copy of the row order: every change of the queue gives a
    // new copy. The swap must reach the copy that is on screen now.
    val onSwap by rememberUpdatedState<(Int, Int) -> Unit> { from, to ->
        rows = rows.toMutableList().apply { add(to, removeAt(from)) }
    }
    val reorder = remember(list) { ReorderState(list) { from, to -> onSwap(from, to) } }

    // The row goes back in line when the queue changes during a drag.
    LaunchedEffect(state.items) { reorder.end() }

    LazyColumn(state = list, modifier = modifier.fillMaxSize()) {
        itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
            QueueRow(
                item = row.item,
                isCurrent = row.key == currentKey,
                drag = RowDrag(
                    isDragged = reorder.draggedKey == row.key,
                    offset = { reorder.offset },
                    onStart = {
                        dragStart = index
                        dragItems = state.items
                        reorder.start(row.key)
                    },
                    onDrag = reorder::dragBy,
                    onEnd = {
                        val end = rows.indexOfFirst { it.key == row.key }
                        reorder.end()
                        // A queue that changed under the finger: the place the drag started from is no longer true.
                        // The same items in a new list are no change: the player sends one at every new song.
                        if (dragItems == state.items && end >= 0 && end != dragStart) onMove(dragStart, end)
                    },
                ),
                actions = RowActions(
                    onPlay = { actions.onPlay(index) },
                    onRemove = { actions.onRemove(index) },
                    onMoveUp = { actions.onMove(index, index - 1) }.takeIf { index > 0 },
                    onMoveDown = { actions.onMove(index, index + 1) }.takeIf { index < rows.lastIndex },
                ),
                modifier = if (reorder.draggedKey == row.key) Modifier else Modifier.animateItem(),
            )
        }
    }
}
