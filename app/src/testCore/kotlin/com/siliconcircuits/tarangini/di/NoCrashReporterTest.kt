/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.di

import com.siliconcircuits.tarangini.core.domain.premium.ReportedProblem
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NoCrashReporterTest {
    @Test
    fun `the reporter of the public build takes every report and is built from nothing`() {
        val reporter = NoCrashReporter()

        ReportedProblem.entries.forEach { reporter.report(it, "ANY_KIND") }

        // It is built from nothing, so it has nothing to send a report with.
        assertEquals(0, NoCrashReporter::class.java.declaredConstructors.single().parameterCount)
    }

    @Test
    fun `the public build ships no Firebase or Play services library`() {
        val assets = RuntimeEnvironment.getApplication().assets
        val list = JSONArray(assets.open("app/cash/licensee/artifacts.json").bufferedReader().use { it.readText() })
        val groups = (0 until list.length()).map { list.getJSONObject(it).getString("groupId") }
        val closed = groups.filter { it.startsWith("com.google.firebase") || it.startsWith("com.google.android.gms") }

        assertTrue("The list of libraries is empty", groups.isNotEmpty())
        assertEquals(emptyList<String>(), closed)
    }
}
