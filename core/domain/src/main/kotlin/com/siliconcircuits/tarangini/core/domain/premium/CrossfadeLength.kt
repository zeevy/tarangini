/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.premium

import kotlinx.coroutines.flow.Flow

/**
 * How long the crossfade between two songs is. The core build has none; the Play build reads the
 * setting of the person. The player does the fade itself, so the hook only gives the length.
 */
fun interface CrossfadeLength {
    /** The length in milliseconds, 0 for no crossfade, given again each time it changes. */
    fun millis(): Flow<Int>
}
