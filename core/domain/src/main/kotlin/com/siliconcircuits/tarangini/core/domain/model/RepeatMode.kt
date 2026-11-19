/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/** What the player does when an item or the queue ends. */
enum class RepeatMode {
    /** Stop after the last item. */
    OFF,

    /** Play the current item again and again. */
    ONE,

    /** Start the queue again after the last item. */
    ALL,
}
