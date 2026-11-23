/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.audio.AudioLevels
import com.siliconcircuits.tarangini.core.domain.audio.LevelReading
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The level meter of the now playing card. It has its own view model, so the meter is read only
 * while the card is on screen, and its 25 readings a second do not make the rest of the screen new.
 */
@HiltViewModel
internal class LevelsViewModel @Inject constructor(levels: AudioLevels) : ViewModel() {
    /** The level now. It stops being read a moment after the screen is gone. */
    val levels: StateFlow<LevelReading> = levels.levels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), LevelReading.SILENCE)

    private companion object {
        // Long enough for a turn of the screen, short enough that a closed screen costs nothing.
        const val STOP_AFTER_MS = 1_000L
    }
}
