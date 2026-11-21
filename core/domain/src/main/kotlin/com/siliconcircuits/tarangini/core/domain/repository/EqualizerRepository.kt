/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import kotlinx.coroutines.flow.Flow

/** The saved equalizer. The playback service follows it, so a change is heard at once, also in the car. */
interface EqualizerRepository {
    /** The saved settings, and each change of them. */
    fun settings(): Flow<EqualizerSettings>

    /** Saves [settings]. */
    suspend fun save(settings: EqualizerSettings)
}
