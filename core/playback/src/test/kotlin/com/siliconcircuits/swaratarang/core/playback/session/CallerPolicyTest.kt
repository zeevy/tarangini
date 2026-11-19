/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerPolicyTest {
    private val policy = CallerPolicy(ownPackage = "our.app", ownUid = 10_001)

    @Test
    fun `this app may connect`() {
        assertTrue(policy.allows("our.app", uid = 10_001, trustedBySystem = false))
    }

    @Test
    fun `a caller the system trusts for media control may connect`() {
        assertTrue(policy.allows("com.android.systemui", uid = 10_050, trustedBySystem = true))
    }

    @Test
    fun `an unknown app is refused`() {
        assertFalse(policy.allows("other.app", uid = 10_002, trustedBySystem = false))
    }

    @Test
    fun `an app that only uses our package name is refused`() {
        assertFalse(policy.allows("our.app", uid = 10_002, trustedBySystem = false))
    }

    @Test
    fun `another package under our user id is refused`() {
        assertFalse(policy.allows("other.app", uid = 10_001, trustedBySystem = false))
    }
}
