/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleRulesTest {
    private fun main(from: String, to: String) = ModuleRules.violation(from, to, isTestConfiguration = false)

    @Test
    fun `a feature may use the domain and the shared ui`() {
        assertNull(main(":feature:library", ":core:domain"))
        assertNull(main(":feature:library", ":core:ui"))
    }

    @Test
    fun `a feature may not use another feature`() {
        assertEquals(
            ":feature:library must not depend on :feature:radio",
            main(":feature:library", ":feature:radio"),
        )
    }

    @Test
    fun `a feature may not use the data or playback module`() {
        assertNotNull(main(":feature:radio", ":core:data"))
        assertNotNull(main(":feature:player", ":core:playback"))
    }

    @Test
    fun `the domain depends on nothing`() {
        assertNotNull(main(":core:domain", ":core:data"))
        assertNotNull(main(":core:domain", ":core:ui"))
    }

    @Test
    fun `data and playback use the domain but not each other`() {
        assertNull(main(":core:data", ":core:domain"))
        assertNull(main(":core:playback", ":core:domain"))
        assertNotNull(main(":core:data", ":core:playback"))
        assertNotNull(main(":core:playback", ":core:data"))
    }

    @Test
    fun `the app may use every module`() {
        assertNull(main(":app", ":feature:home"))
        assertNull(main(":app", ":core:data"))
        assertNull(main(":app", ":core:playback"))
    }

    @Test
    fun `the shared fakes are for tests only`() {
        assertNull(ModuleRules.violation(":feature:home", ":core:testing", isTestConfiguration = true))
        assertNotNull(main(":feature:home", ":core:testing"))
        assertNotNull(main(":app", ":core:testing"))
    }

    @Test
    fun `a test configuration still follows the table`() {
        assertNotNull(ModuleRules.violation(":feature:home", ":core:data", isTestConfiguration = true))
    }

    @Test
    fun `a module that is not in the table is refused`() {
        assertEquals(
            ":core:network is not a module in docs/ARCHITECTURE.md",
            main(":core:network", ":core:domain"),
        )
    }

    @Test
    fun `the premium module may use the domain and the shared ui only`() {
        assertNull(main(":premium", ":core:domain"))
        assertNull(main(":premium", ":core:ui"))
        assertNotNull(main(":premium", ":core:data"))
    }

    @Test
    fun `only test configurations count as tests`() {
        assertTrue(ModuleRules.isTestConfiguration("testImplementation"))
        assertTrue(ModuleRules.isTestConfiguration("testDebugImplementation"))
        assertTrue(ModuleRules.isTestConfiguration("androidTestImplementation"))
        assertFalse(ModuleRules.isTestConfiguration("implementation"))
        assertFalse(ModuleRules.isTestConfiguration("latestImplementation"))
        assertFalse(ModuleRules.isTestConfiguration("debugImplementation"))
    }
}
