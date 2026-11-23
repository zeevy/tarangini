/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortBandNameTest {
    @Test
    fun `band names are short and use a point in every language`() {
        val before = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            assertEquals(
                listOf("60", "910", "1k", "3.6k", "14k"),
                listOf(60f, 910f, 1_000f, 3_600f, 14_000f).map(::shortBandName),
            )
        } finally {
            Locale.setDefault(before)
        }
    }
}
