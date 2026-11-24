/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import com.siliconcircuits.tarangini.core.domain.premium.CrossfadeLength
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The core build has no crossfade: songs follow one another as they are, with no gap. */
internal class NoCrossfade @Inject constructor() : CrossfadeLength {
    override fun millis(): Flow<Int> = flowOf(0)
}
