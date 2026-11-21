/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import com.siliconcircuits.tarangini.core.domain.audio.FIVE_BANDS_HZ
import org.junit.Assert.assertEquals
import org.junit.Test

class FiveEqualizerBandsTest {
    @Test
    fun `the core build has the five bands from low to high`() {
        val bands = FiveEqualizerBands().centresHz()

        assertEquals(FIVE_BANDS_HZ, bands)
        assertEquals(bands.sorted(), bands)
    }
}
