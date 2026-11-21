/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.premium

/**
 * The bands of the equalizer. The core build has the five bands of `FIVE_BANDS_HZ`; the Play build
 * can offer ten.
 */
fun interface EqualizerBands {
    /** The centre of each band in Hz, from low to high. */
    fun centresHz(): List<Float>
}
