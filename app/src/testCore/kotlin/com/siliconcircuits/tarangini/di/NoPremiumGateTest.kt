/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
