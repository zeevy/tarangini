/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/** What the sleep timer does right now. */
sealed interface SleepState {
    /** No timer is set. */
    data object Off : SleepState

    /**
     * Playback stops after a time.
     *
     * @property leftMs the time left, counted down once a second
     */
    data class Timed(val leftMs: Long) : SleepState

    /** Playback stops when the item that plays has ended. */
    data object EndOfTrack : SleepState
}
