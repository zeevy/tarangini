/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.ListCard
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.player.R
import kotlinx.coroutines.launch

/**
 * Up next on its own list card: the songs left and their time, Save as a playlist and Clear the
 * queue, then the song that plays, kept at the top while the list scrolls, and every song after
 * it. Songs already played are left out. A swipe to the left takes a song out; a hold and a drag
 * moves it. With shuffle on the rows follow the order the player picked, and cannot be moved.
 *
 * @param hidden the place of a song that was taken out and can still come back with Undo
 */
@Composable
internal fun UpNextCard(
    state: QueueUiState,
    actions: QueueActions,
    list: LazyListState,
    modifier: Modifier = Modifier,
    hidden: Int? = null,
) {
    val current = state.currentIndex
    val rows = upNextPlaces(state).filter { it != hidden }
    val left = (rows.size - 1).coerceAtLeast(0)
    val leftMs = rows.drop(1).sumOf { state.items[it].durationMs ?: 0L }
    val canChange = current != null && state.items.getOrNull(current)?.isStream == false
    ListCard(
        count = if (current == null) {
            stringResource(R.string.up_next_queue_empty)
        } else {
            stringResource(R.string.up_next_count, left, formatDuration(leftMs).ifEmpty { "0:00" })
        },
        modifier = modifier,
        tools = {
            if (canChange) {
                Tool(CoreUiR.drawable.ic_playlist_add, stringResource(R.string.up_next_save), actions.onSave)
                Tool(CoreUiR.drawable.ic_clear_queue, stringResource(R.string.up_next_clear), actions.onClear)
            }
        },
    ) {
        if (current == null) {
            EmptyQueue(Modifier.fillMaxSize())
        } else {
            UpNextList(state, rows, actions, list, canMove = canChange && state.playOrder == null)
        }
    }
}

/** The places of the song that plays and of every song after it, in the order they play. */
internal fun upNextPlaces(state: QueueUiState): List<Int> {
    val current = state.currentIndex?.takeIf { it in state.items.indices } ?: return emptyList()
    return state.playOrder?.filter { it in state.items.indices } ?: (current until state.items.size).toList()
}

@Composable
private fun UpNextList(
    state: QueueUiState,
    rows: List<Int>,
    actions: QueueActions,
    list: LazyListState,
    canMove: Boolean,
) {
    val places by rememberUpdatedState(rows)
    val move by rememberUpdatedState(actions.onMove)
    // The list hands the places of the rows on screen; the queue needs places in the queue.
    val reorder = remember(list) {
        ReorderState(list) { from, to ->
            // The song that plays stays at the top: nothing moves onto it or away from it.
            val movable = places.drop(1)
            val a = places.getOrNull(from)?.takeIf { it in movable }
            val b = places.getOrNull(to)?.takeIf { it in movable }
            if (a != null && b != null) move(a, b)
        }
    }
    val keys = rowKeys(state.items)
    LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
        val playing = rows.firstOrNull() ?: return@LazyColumn
        stickyHeader(key = keys[playing]) {
            UpNextRow(playing, state.items[playing], isCurrent = true, onPlay = {
            }, modifier = Modifier.background(lookColors.list))
        }
        items(count = rows.size - 1, key = { keys[rows[it + 1]] }) { row ->
            val place = rows[row + 1]
            val key = keys[place]
            val dragged = reorder.draggedKey == key
            RemovableRow(onRemove = { actions.onRemove(place) }) {
                UpNextRow(
                    place,
                    state.items[place],
                    isCurrent = false,
                    onPlay = { actions.onPlay(place) },
                    modifier = Modifier
                        .then(if (canMove) Modifier.dragToMove(key, reorder) else Modifier)
                        .graphicsLayer { translationY = if (dragged) reorder.offset else 0f }
                        .then(if (dragged) Modifier.shadow(4.dp, MaterialTheme.shapes.extraSmall) else Modifier),
                    moves = if (canMove) {
                        RowMoves(row > 0, row < rows.size - 2, { move(place, place - 1) }, {
                            move(
                                place,
                                place + 1,
                            )
                        })
                    } else {
                        null
                    },
                    onRemove = { actions.onRemove(place) },
                )
            }
        }
    }
}

/** A hold and then a drag moves the row; each change of place gives a short vibration. */
@Composable
private fun Modifier.dragToMove(key: Any, reorder: ReorderState): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                reorder.start(key)
            },
            onDrag = { change, amount ->
                change.consume()
                reorder.dragBy(amount.y)
            },
            onDragEnd = reorder::end,
            onDragCancel = reorder::end,
        )
    }
}

/** A row that a swipe to the left takes out. Behind it the danger colour says Remove. */
@Composable
private fun RemovableRow(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val swipe = rememberSwipeToDismissBoxState()
    val look = lookColors
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart) onRemove()
            // The row is kept by its key; it must stand in place when Undo brings it back.
            scope.launch { swipe.snapTo(SwipeToDismissBoxValue.Settled) }
        },
        backgroundContent = {
            // Drawn only during a swipe: at rest it would show at the round corners of the row.
            if (swipe.dismissDirection != SwipeToDismissBoxValue.EndToStart) return@SwipeToDismissBox
            Row(
                modifier = Modifier.fillMaxSize().background(look.danger, MaterialTheme.shapes.extraSmall),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(CoreUiR.drawable.ic_delete), null, Modifier.size(18.dp), look.onSnack)
                    Text(
                        stringResource(R.string.queue_remove_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = look.onSnack,
                    )
                }
            }
        },
        content = { content() },
    )
}

/** Up and down moves of a row for TalkBack, as a drag needs sight. */
private class RowMoves(val canUp: Boolean, val canDown: Boolean, val onUp: () -> Unit, val onDown: () -> Unit)

/**
 * One song: its place in the queue, title and artist on one line, its length and a grip. The song
 * that plays stands out in the accent colour on a lit row.
 */
@Composable
private fun UpNextRow(
    index: Int,
    item: PlaybackItem,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    moves: RowMoves? = null,
    onRemove: (() -> Unit)? = null,
) {
    val look = lookColors
    val title = item.title ?: stringResource(R.string.up_next_no_title)
    val length = item.durationMs?.let(::formatDuration)
    val spoken = listOfNotNull(
        stringResource(R.string.up_next_place, index + 1),
        title,
        item.artist,
        length,
        stringResource(R.string.queue_playing_now).takeIf { isCurrent },
    ).joinToString(", ")
    val playLabel = stringResource(CoreUiR.string.action_play)
    val actions = rowActions(moves, onRemove)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isCurrent) look.rowOn else look.list, MaterialTheme.shapes.extraSmall)
            .heightIn(min = RowHeight)
            .then(if (isCurrent) Modifier else Modifier.clickable(onClickLabel = playLabel, onClick = onPlay))
            .clearAndSetSemantics {
                contentDescription = spoken
                if (!isCurrent) {
                    onClick(playLabel) {
                        onPlay()
                        true
                    }
                }
                if (actions.isNotEmpty()) customActions = actions
            }
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val digits = TextStyle(fontFamily = DigitsFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        val number = if (isCurrent) look.accentOnList else look.listMuted
        Text(text = (index + 1).toString(), modifier = Modifier.width(22.dp), style = digits, color = number)
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isCurrent) look.accentOnList else look.listText,
                    ),
                ) { append(title) }
                item.artist?.let { artist ->
                    withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = look.listMuted)) { append(" ", artist) }
                }
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        length?.let { Text(text = it, style = digits.copy(fontWeight = FontWeight.SemiBold), color = look.listMuted) }
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            if (moves != null) {
                Icon(painterResource(CoreUiR.drawable.ic_drag_handle), null, Modifier.size(16.dp), look.listMuted)
            }
        }
    }
}

@Composable
private fun rowActions(moves: RowMoves?, onRemove: (() -> Unit)?): List<CustomAccessibilityAction> {
    val up = stringResource(R.string.queue_move_up)
    val down = stringResource(R.string.queue_move_down)
    val remove = stringResource(R.string.queue_remove)
    return buildList {
        if (moves?.canUp == true) add(CustomAccessibilityAction(up) { moves.onUp().let { true } })
        if (moves?.canDown == true) add(CustomAccessibilityAction(down) { moves.onDown().let { true } })
        if (onRemove != null) add(CustomAccessibilityAction(remove) { onRemove().let { true } })
    }
}

@Composable
private fun Tool(icon: Int, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 48.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = lookColors.listMuted,
        )
    }
}

@Composable
private fun EmptyQueue(modifier: Modifier = Modifier) {
    val look = lookColors
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Icon(painterResource(CoreUiR.drawable.ic_queue), null, Modifier.size(30.dp), look.listMuted)
        Text(
            stringResource(R.string.queue_empty_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = look.listText,
        )
        Text(
            stringResource(R.string.queue_empty_text),
            style = MaterialTheme.typography.bodySmall,
            color = look.listMuted,
        )
    }
}

/**
 * A key for each place that stays with its song when songs move: the id of the song and how often
 * it came before in the queue, as the same song can be in a queue twice.
 */
private fun rowKeys(items: List<PlaybackItem>): List<String> {
    val seen = HashMap<String, Int>()
    return items.map { item ->
        val count = seen.merge(item.id, 1, Int::plus) ?: 1
        "${item.id}#$count"
    }
}

private val RowHeight = 48.dp
