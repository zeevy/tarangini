/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class TaranginiBuildTest {
    @Test
    fun `the app uses the base package`() {
        assertEquals("com.siliconcircuits.tarangini", TaranginiBuild.namespaceFor(":app"))
    }

    @Test
    fun `a module namespace follows its path`() {
        assertEquals(
            "com.siliconcircuits.tarangini.feature.library",
            TaranginiBuild.namespaceFor(":feature:library"),
        )
        assertEquals("com.siliconcircuits.tarangini.core.data", TaranginiBuild.namespaceFor(":core:data"))
    }

    @Test
    fun `only the app module itself maps to the base package`() {
        assertEquals("com.siliconcircuits.tarangini.feature.app", TaranginiBuild.namespaceFor(":feature:app"))
    }
}
