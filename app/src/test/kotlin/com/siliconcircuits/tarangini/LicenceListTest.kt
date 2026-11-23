/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The open source licences screen needs the list of the build and a text for every licence in it. */
@RunWith(RobolectricTestRunner::class)
class LicenceListTest {
    private val assets = RuntimeEnvironment.getApplication().assets

    private fun hasText(licenceId: String) = runCatching { assets.open("licences/$licenceId.txt").close() }.isSuccess

    @Test
    fun `the app holds the list of its libraries, and the text of every licence in it`() {
        val list = JSONArray(assets.open("app/cash/licensee/artifacts.json").bufferedReader().use { it.readText() })
        val ids = (0 until list.length()).mapNotNull { index ->
            list.getJSONObject(index).optJSONArray("spdxLicenses")?.optJSONObject(0)?.optString("identifier")
        }.toSet()

        assertTrue("The list of libraries is empty", ids.isNotEmpty())
        assertTrue("No text for ${ids.filterNot(::hasText)}", ids.all(::hasText))
    }

    @Test
    fun `the app holds the notices the build collected, each for a library of the list`() {
        val list = JSONArray(assets.open("app/cash/licensee/artifacts.json").bufferedReader().use { it.readText() })
        val libraries = (0 until list.length()).map { index ->
            list.getJSONObject(index).let { it.optString("groupId") + ":" + it.optString("artifactId") }
        }.toSet()
        val notices = JSONArray(assets.open("licences/notices.json").bufferedReader().use { it.readText() })
        val withNotice = (0 until notices.length()).map { index ->
            notices.getJSONObject(index).let { it.optString("groupId") + ":" + it.optString("artifactId") }
        }

        // A notice of a library that is not in the list would never show on the licences screen.
        assertTrue("No notice: the build step did not run, or no library has one any more", withNotice.isNotEmpty())
        assertTrue("Not in the list: ${withNotice - libraries}", libraries.containsAll(withNotice))
    }

    @Test
    fun `the font has its licence text`() {
        assertTrue(hasText("Manrope-OFL"))
        assertTrue(hasText("JetBrainsMono-OFL"))
    }
}
