/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.settings

import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.repository.EqualizerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** The equalizer for tests, kept in memory. */
class FakeEqualizerRepository(first: EqualizerSettings = EqualizerSettings()) : EqualizerRepository {
    /** The saved settings. A test can read and set them. */
    val saved = MutableStateFlow(first)

    /** How many times [save] was called. */
    var saves = 0
        private set

    override fun settings(): Flow<EqualizerSettings> = saved

    override suspend fun save(settings: EqualizerSettings) {
        saves++
        saved.value = settings
    }
}
