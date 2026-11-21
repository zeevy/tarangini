/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/** The network rules of the finished app, with the resources of all its modules joined. */
@RunWith(RobolectricTestRunner::class)
class NetworkSecurityTest {
    private val app: Application = RuntimeEnvironment.getApplication()

    /** The value of `cleartextTrafficPermitted` of each rule in the network security settings. */
    private fun plainHttpRules(): List<String?> {
        val id = app.resources.getIdentifier("network_security_config", "xml", app.packageName)
        assertNotEquals("The app has no network security settings", 0, id)
        val rules = mutableListOf<String?>()
        app.resources.getXml(id).use { xml ->
            while (xml.next() != XmlPullParser.END_DOCUMENT) {
                val isRule =
                    xml.eventType == XmlPullParser.START_TAG && xml.name in setOf("base-config", "domain-config")
                if (isRule) rules += xml.getAttributeValue(null, "cleartextTrafficPermitted")
            }
        }
        return rules
    }

    private fun hasRadio(): Boolean =
        runCatching { Class.forName("com.siliconcircuits.tarangini.feature.radio.RadioScreenKt") }.isSuccess

    @Test
    fun `an app with no radio allows no plain http at all`() {
        // Only radio streams need plain http. When the app gets radio again, this rule changes with it.
        if (!hasRadio()) assertEquals(listOf<String?>("false"), plainHttpRules())
    }
}
