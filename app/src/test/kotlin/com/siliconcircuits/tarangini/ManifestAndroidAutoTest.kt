/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

/** What Android Auto needs from the finished app to show it in the car, with the manifests of all modules joined. */
@RunWith(RobolectricTestRunner::class)
class ManifestAndroidAutoTest {
    private val app: Application = RuntimeEnvironment.getApplication()

    @Test
    fun `the app tells Android Auto that it is a media app, and nothing else`() {
        val info = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
        val descriptor = info.metaData.getInt("com.google.android.gms.car.application")
        val uses = mutableListOf<String>()
        val parser = app.resources.getXml(descriptor)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "uses") {
                uses += parser.getAttributeValue(null, "name")
            }
        }

        assertEquals(listOf("media"), uses)
    }

    @Test
    fun `the car finds the playback service as a media browser`() {
        val found = app.packageManager.queryIntentServices(
            Intent("android.media.browse.MediaBrowserService").setPackage(app.packageName),
            0,
        )

        assertTrue(found.any { it.serviceInfo.name.endsWith(".PlaybackService") && it.serviceInfo.exported })
    }
}
