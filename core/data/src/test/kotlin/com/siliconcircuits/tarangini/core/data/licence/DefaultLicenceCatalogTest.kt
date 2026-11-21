/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.licence

import com.siliconcircuits.tarangini.core.domain.model.LibraryLicence
import com.siliconcircuits.tarangini.core.domain.model.OpenSourceLibrary
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DefaultLicenceCatalogTest {
    private val apache = LibraryLicence("Apache-2.0", "Apache License 2.0")

    @Test
    fun `the list of the build is read, with the name or else the coordinates`() {
        val json = """
            [
              {"groupId": "androidx.activity", "artifactId": "activity", "version": "1.13.0", "name": "Activity",
               "spdxLicenses": [{"identifier": "Apache-2.0", "name": "Apache License 2.0", "url": "https://x"}]},
              {"groupId": "com.example", "artifactId": "plain", "version": "2",
               "spdxLicenses": [{"identifier": "MIT", "name": "MIT License"}, {"identifier": "Apache-2.0"}]},
              {"groupId": "com.example", "artifactId": "no-licence", "version": "1"},
              "not an entry"
            ]
        """.trimIndent()

        assertEquals(
            listOf(
                OpenSourceLibrary("Activity", "1.13.0", apache),
                OpenSourceLibrary("com.example:plain", "2", LibraryLicence("MIT", "MIT License")),
            ),
            parseArtifacts(json),
        )
    }

    @Test
    fun `a library gets the notice the build collected for it, cleaned`() {
        val libraries = """
            [{"groupId": "org.a", "artifactId": "one", "version": "1", "spdxLicenses": [{"identifier": "Apache-2.0"}]},
             {"groupId": "org.a", "artifactId": "two", "version": "1", "spdxLicenses": [{"identifier": "Apache-2.0"}]}]
        """.trimIndent()
        val notices = """
            [{"groupId": "org.a", "artifactId": "one", "notice": " Made\u0007 by\tA\u202E\nIn 2020 "},
             {"groupId": "org.a", "artifactId": "empty", "notice": "  "}, "no entry"]
        """.trimIndent()

        val read = parseNotices(notices)

        assertEquals(mapOf("org.a:one" to "Made by A\nIn 2020"), read)
        assertEquals(listOf("Made by A\nIn 2020", null), parseArtifacts(libraries, read).map { it.notice })
        assertEquals(emptyMap<String, String>(), parseNotices("{}"))
    }

    @Test
    fun `a list that is no list gives nothing`() {
        assertEquals(emptyList<OpenSourceLibrary>(), parseArtifacts("{}"))
        assertEquals(emptyList<OpenSourceLibrary>(), parseArtifacts(""))
    }

    @Test
    fun `with no list of the build the font is still listed, and an id that is no licence id reads nothing`() =
        runTest {
            val app = RuntimeEnvironment.getApplication()
            val catalog = DefaultLicenceCatalog(app, StandardTestDispatcher(testScheduler))

            assertTrue(catalog.libraries().any { it.name == "Manrope" })
            assertNull(catalog.text("../licences/Apache-2.0"))
            assertNull(catalog.text(""))
            assertNull(catalog.text("No-Such-Licence"))
        }
}
