/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Follows one row of a list while a finger drags it up or down.
 *
 * The dragged row is drawn at the place of the finger. When its middle passes into another row, the
 * two change places in the list through [onSwap], and the drawing offset is corrected so that the
 * row stays under the finger.
 *
 * It assumes rows of about the same height, and it does not scroll the list when the finger
 * reaches its edge: a far move is done in steps, or with the "move up" and "move down" actions.
 *
 * @param onSwap asks to exchange the rows at two places of the list
 */
@Stable
internal class ReorderState(private val list: LazyListState, private val onSwap: (from: Int, to: Int) -> Unit) {
    /** The key of the row that is dragged, or null. */
    var draggedKey by mutableStateOf<Any?>(null)
        private set

    /** How far the dragged row is drawn from its place in the list, in pixels. */
    var offset by mutableFloatStateOf(0f)
        private set

    /** The place the dragged row has after the last swap. The list has it there one frame later. */
    private var placeAfterSwap: Int? = null

    fun start(key: Any) {
        draggedKey = key
        offset = 0f
        placeAfterSwap = null
    }

    fun dragBy(pixels: Float) {
        offset += pixels
        val rows = list.layoutInfo.visibleItemsInfo
        val dragged = rows.firstOrNull { it.key == draggedKey }
            // Until the list has laid the last swap out, its places are the old ones.
            ?.takeIf { placeAfterSwap == null || it.index == placeAfterSwap }
            ?: return
        val middle = dragged.offset + offset + dragged.size / 2f
        val over = rows.firstOrNull { it.key != draggedKey && middle > it.offset && middle < it.offset + it.size }
            ?: return
        onSwap(dragged.index, over.index)
        placeAfterSwap = over.index
        // The place of the dragged row moved by the height of the row it passed.
        offset += if (over.index > dragged.index) -over.size else over.size
    }

    fun end() {
        draggedKey = null
        offset = 0f
        placeAfterSwap = null
    }
}
