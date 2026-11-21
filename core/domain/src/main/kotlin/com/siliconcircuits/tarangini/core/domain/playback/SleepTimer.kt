/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.playback

import com.siliconcircuits.tarangini.core.domain.model.SleepState
import kotlinx.coroutines.flow.StateFlow

/**
 * Stops playback after a time or at the end of the item that plays. The timer lives with the
 * player and not with a screen, so it goes on while the app is closed. A new timer takes the
 * place of the one before.
 */
interface SleepTimer {
    /** What the timer does right now. */
    val state: StateFlow<SleepState>

    /** Pauses after [minutes], with a short fade out. The value is held between 1 and [MAX_MINUTES]. */
    fun stopAfter(minutes: Int)

    /** Pauses when the item that plays has ended, before the next one starts. */
    fun stopAtEndOfTrack()

    /** Takes the timer away. Playback goes on. */
    fun cancel()

    companion object {
        /** The longest time a timer can be set to: twelve hours. */
        const val MAX_MINUTES = 720
    }
}
