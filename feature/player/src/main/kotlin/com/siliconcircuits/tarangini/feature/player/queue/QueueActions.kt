/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.queue

/** What the person can do with the queue. A place is the index of an item in the queue. */
internal data class QueueActions(
    val onPlay: (index: Int) -> Unit,
    val onMove: (from: Int, to: Int) -> Unit,
    val onRemove: (index: Int) -> Unit,
)
