/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import com.siliconcircuits.tarangini.core.domain.audio.FIVE_BANDS_HZ
import com.siliconcircuits.tarangini.core.domain.premium.EqualizerBands
import javax.inject.Inject

/** The five bands of the core build. */
internal class FiveEqualizerBands @Inject constructor() : EqualizerBands {
    override fun centresHz(): List<Float> = FIVE_BANDS_HZ
}
