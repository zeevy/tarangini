/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.permission

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPermissionStateTest {
    @Test
    fun `a granted permission is granted, whatever else Android says`() {
        listOf(0, 1, 2).forEach { silentRefusals ->
            listOf(false, true).forEach { canAskAgain ->
                assertEquals(
                    AudioPermissionState.GRANTED,
                    audioPermissionState(granted = true, silentRefusals = silentRefusals, canAskAgain = canAskAgain),
                )
            }
        }
    }

    @Test
    fun `before any request the user was not asked`() {
        assertEquals(
            AudioPermissionState.NOT_ASKED,
            audioPermissionState(granted = false, silentRefusals = 0, canAskAgain = false),
        )
    }

    @Test
    fun `a no that Android will ask about again is a plain no, also when it is from an earlier visit`() {
        listOf(0, 1, 2).forEach { silentRefusals ->
            assertEquals(
                AudioPermissionState.DENIED,
                audioPermissionState(granted = false, silentRefusals = silentRefusals, canAskAgain = true),
            )
        }
    }

    @Test
    fun `one refusal with no word from Android keeps the button that asks`() {
        assertEquals(
            AudioPermissionState.DENIED,
            audioPermissionState(granted = false, silentRefusals = 1, canAskAgain = false),
        )
    }

    @Test
    fun `two refusals with no word from Android are for good`() {
        assertEquals(
            AudioPermissionState.DENIED_FOR_GOOD,
            audioPermissionState(granted = false, silentRefusals = 2, canAskAgain = false),
        )
    }

    @Test
    fun `a first no is not silent`() {
        assertEquals(0, silentRefusalsAfter(before = 0, couldAskAgain = false, canAskAgain = true))
        assertEquals(0, silentRefusalsAfter(before = 1, couldAskAgain = false, canAskAgain = true))
    }

    @Test
    fun `the second no to the question of Android is for good at once`() {
        assertEquals(2, silentRefusalsAfter(before = 0, couldAskAgain = true, canAskAgain = false))
    }

    @Test
    fun `a question closed without an answer counts one, and a second one in a row reaches for good`() {
        val first = silentRefusalsAfter(before = 0, couldAskAgain = false, canAskAgain = false)
        val second = silentRefusalsAfter(before = first, couldAskAgain = false, canAskAgain = false)

        assertEquals(1, first)
        assertEquals(SILENT_REFUSALS_FOR_GOOD, second)
    }

    @Test
    fun `Android 12 and 12L use the permission for all files`() {
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", audioPermissionFor(sdk = 31))
        assertEquals("android.permission.READ_EXTERNAL_STORAGE", audioPermissionFor(sdk = 32))
    }

    @Test
    fun `Android 13 and above use the permission for audio alone`() {
        assertEquals("android.permission.READ_MEDIA_AUDIO", audioPermissionFor(sdk = 33))
        assertEquals("android.permission.READ_MEDIA_AUDIO", audioPermissionFor(sdk = 37))
    }

    @Test
    fun `the permission for all files is asked for only up to Android 12L`() {
        val manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            File("src/main/AndroidManifest.xml"),
        )
        val asked = manifest.getElementsByTagName("uses-permission")
        val entries = List(asked.length) { asked.item(it).attributes }
            .associate {
                it.getNamedItem("android:name").nodeValue to
                    it.getNamedItem("android:maxSdkVersion")?.nodeValue
            }

        assertEquals(
            mapOf("android.permission.READ_MEDIA_AUDIO" to null, "android.permission.READ_EXTERNAL_STORAGE" to "32"),
            entries,
        )
    }
}
