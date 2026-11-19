/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import org.junit.Assert.assertFalse
import org.junit.Test

class NoPremiumGateTest {
    @Test
    fun `the core build never unlocks premium features`() {
        assertFalse(NoPremiumGate().isUnlocked)
    }
}
