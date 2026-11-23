/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.lazy.LazyListState

/** True once the list is moved away from its top. */
internal fun LazyListState.isScrolled(): Boolean = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0

/** How far the list can scroll in all, from the rows on screen: what is above them and below them. */
internal fun LazyListState.scrollRange(): Int {
    val rows = layoutInfo.visibleItemsInfo
    if (rows.isEmpty()) return 0
    val rowPx = rows.sumOf { it.size } / rows.size
    val last = rows.last()
    val below = (layoutInfo.totalItemsCount - 1 - last.index) * rowPx +
        (last.offset + last.size - layoutInfo.viewportEndOffset).coerceAtLeast(0)
    return firstVisibleItemIndex * rowPx + firstVisibleItemScrollOffset + below
}
