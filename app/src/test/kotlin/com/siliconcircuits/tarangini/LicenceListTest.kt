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
    fun `every licence the build allows has its text, and so has the font`() {
        // The same ids as the allow list of the licence check in the application build plugin.
        val allowed = listOf("Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "MPL-2.0", "Manrope-OFL")

        assertTrue("No text for ${allowed.filterNot(::hasText)}", allowed.all(::hasText))
    }
}
