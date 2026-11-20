/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.siliconcircuits.tarangini.core.domain.model.PlaybackItem
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumArt
import com.siliconcircuits.tarangini.core.ui.theme.Spacing
import com.siliconcircuits.tarangini.feature.player.ItemTexts
import com.siliconcircuits.tarangini.feature.player.R
import kotlinx.coroutines.delay

/**
 * The drag of one row by its handle.
 *
 * @property offset how far the row is drawn from its place while it is dragged, in pixels. It is read
 *   only when the row is drawn, so a drag does not build the row again for every pixel
 */
internal class RowDrag(
    val isDragged: Boolean,
    val offset: () -> Float,
    val onStart: () -> Unit,
    val onDrag: (Float) -> Unit,
    val onEnd: () -> Unit,
)

/** What can be done with one row. A move that is not possible, such as "up" on the first row, is null. */
internal class RowActions(
    val onPlay: () -> Unit,
    val onRemove: () -> Unit,
    val onMoveUp: (() -> Unit)?,
    val onMoveDown: (() -> Unit)?,
)

/**
 * One item of the queue. A tap plays it, a swipe to either side takes it out, and the handle at its
 * end drags it to another place. A screen reader gets the same as actions of the row.
 */
@Composable
internal fun QueueRow(
    item: PlaybackItem,
    isCurrent: Boolean,
    drag: RowDrag,
    actions: RowActions,
    modifier: Modifier = Modifier,
) {
    val swipe = rememberSwipeToDismissBoxState()
    // A row that was swiped away stays hidden. When the player takes the item out, the row goes with
    // it. When the row is still here a moment later, the player kept the item, or the row now shows
    // another copy of the same song, so it is put back.
    // The settled value: the current one already changes under the finger, half way through a swipe.
    LaunchedEffect(swipe.settledValue) {
        if (swipe.settledValue != SwipeToDismissBoxValue.Settled) {
            delay(SHOW_AGAIN_AFTER_MS)
            swipe.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state = swipe,
        backgroundContent = { Box(Modifier.fillMaxSize()) },
        modifier = modifier
            .zIndex(if (drag.isDragged) 1f else 0f)
            .graphicsLayer { translationY = if (drag.isDragged) drag.offset() else 0f },
        onDismiss = { value -> if (value != SwipeToDismissBoxValue.Settled) actions.onRemove() },
    ) {
        Surface(
            color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            tonalElevation = if (drag.isDragged) Spacing.small else Spacing.none,
        ) {
            RowBody(item, isCurrent, drag, actions)
        }
    }
}

@Composable
private fun RowBody(item: PlaybackItem, isCurrent: Boolean, drag: RowDrag, actions: RowActions) {
    val playingNow = stringResource(R.string.queue_playing_now)
    val rowActions = listOfNotNull(
        actions.onMoveUp?.let { action(stringResource(R.string.queue_move_up), it) },
        actions.onMoveDown?.let { action(stringResource(R.string.queue_move_down), it) },
        action(stringResource(R.string.queue_remove), actions.onRemove),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(CoreUiR.string.action_play), onClick = actions.onPlay)
            .semantics {
                customActions = rowActions
                if (isCurrent) stateDescription = playingNow
            }
            .padding(start = Spacing.medium, top = Spacing.small, bottom = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(item.artworkUri)
        ItemTexts(item.title, item.artist, Modifier.weight(1f).padding(horizontal = Spacing.medium), item.isStream)
        DragHandle(drag)
    }
}

private fun action(label: String, run: () -> Unit) = CustomAccessibilityAction(label) {
    run()
    true
}

/** The handle is for the finger only. A screen reader moves a row with the actions of the row. */
@Composable
private fun DragHandle(drag: RowDrag) {
    // The gesture lives as long as the row. It must call the newest functions, which know the place the row has now.
    val current by rememberUpdatedState(drag)
    Icon(
        painter = painterResource(CoreUiR.drawable.ic_drag_handle),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { current.onStart() },
                    onDragEnd = { current.onEnd() },
                    onDragCancel = { current.onEnd() },
                    onDrag = { change, amount ->
                        change.consume()
                        current.onDrag(amount.y)
                    },
                )
            },
    )
}

private const val SHOW_AGAIN_AFTER_MS = 500L
