/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/** What the display of the player shows under the song. A tap on the visual moves to the next one. */
enum class VisualMode {
    /** Bars of the spectrum, from low to high notes. */
    BARS,

    /** The wave of the newest sound. */
    WAVE,

    /** Nothing moves, which also saves the work of reading the sound. */
    OFF,
}
