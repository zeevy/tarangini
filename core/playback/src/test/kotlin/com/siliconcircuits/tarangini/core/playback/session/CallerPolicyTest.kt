/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import org.junit.Assert.assertEquals
import org.junit.Test

class CallerPolicyTest {
    private val policy = CallerPolicy(ownPackage = "our.app", ownUid = 10_001)

    @Test
    fun `this app may do everything, trusted by the system or not`() {
        assertEquals(CallerAccess.FULL, policy.accessFor("our.app", uid = 10_001, trustedBySystem = false))
        assertEquals(CallerAccess.FULL, policy.accessFor("our.app", uid = 10_001, trustedBySystem = true))
    }

    @Test
    fun `a caller the system trusts for media control gets the transport controls only`() {
        val access = policy.accessFor("com.android.systemui", uid = 10_050, trustedBySystem = true)

        assertEquals(CallerAccess.TRANSPORT_ONLY, access)
    }

    @Test
    fun `an unknown app is refused`() {
        assertEquals(CallerAccess.REFUSED, policy.accessFor("other.app", uid = 10_002, trustedBySystem = false))
    }

    @Test
    fun `an app that only uses our package name is refused`() {
        assertEquals(CallerAccess.REFUSED, policy.accessFor("our.app", uid = 10_002, trustedBySystem = false))
    }

    @Test
    fun `another package under our user id is refused`() {
        assertEquals(CallerAccess.REFUSED, policy.accessFor("other.app", uid = 10_001, trustedBySystem = false))
    }
}
