/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.playback

import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.playback.SleepTimer
import kotlinx.coroutines.flow.MutableStateFlow

/** A sleep timer for tests. It never counts down: a test sets [state] to what it wants to show. */
class FakeSleepTimer : SleepTimer {
    override val state = MutableStateFlow<SleepState>(SleepState.Off)

    override fun stopAfter(minutes: Int) {
        state.value = SleepState.Timed(minutes * MS_PER_MINUTE)
    }

    override fun stopAtEndOfTrack() {
        state.value = SleepState.EndOfTrack
    }

    override fun cancel() {
        state.value = SleepState.Off
    }

    private companion object {
        const val MS_PER_MINUTE = 60_000L
    }
}
