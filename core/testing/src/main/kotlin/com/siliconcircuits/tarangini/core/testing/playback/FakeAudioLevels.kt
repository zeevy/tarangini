/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.playback

import com.siliconcircuits.tarangini.core.domain.audio.AudioLevels
import com.siliconcircuits.tarangini.core.domain.audio.LevelReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Levels for tests. A test sets [reading]; it starts at silence. */
class FakeAudioLevels : AudioLevels {
    /** The level the meter shows now. */
    val reading = MutableStateFlow(LevelReading.SILENCE)

    override fun levels(): Flow<LevelReading> = reading
}
