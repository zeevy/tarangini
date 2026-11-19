/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.tarangini

import android.app.Application
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** What the finished app asks Android for, with the manifests of all its modules joined. */
@RunWith(RobolectricTestRunner::class)
class ManifestPermissionsTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val asked: Set<String> = app.packageManager
        .getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions
        .orEmpty()
        .toSet()

    @Test
    fun `the app asks to read audio files`() {
        assertTrue("android.permission.READ_MEDIA_AUDIO" in asked)
    }

    @Test
    fun `the app never asks to manage all files`() {
        assertFalse("android.permission.MANAGE_EXTERNAL_STORAGE" in asked)
    }

    @Test
    fun `the app asks for no other access to files, pictures or video`() {
        val storage = asked.filter { "STORAGE" in it || "READ_MEDIA" in it }.toSet()

        assertTrue(
            "Unexpected: $storage",
            storage - setOf("android.permission.READ_MEDIA_AUDIO", "android.permission.READ_EXTERNAL_STORAGE") ==
                emptySet<String>(),
        )
    }
}
