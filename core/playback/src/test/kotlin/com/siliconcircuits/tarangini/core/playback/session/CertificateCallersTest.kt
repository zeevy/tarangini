/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSigningInfo

@RunWith(RobolectricTestRunner::class)
class CertificateCallersTest {
    private val packages: PackageManager = RuntimeEnvironment.getApplication().packageManager
    private val carKey = Signature("0a0b0c")
    private val otherKey = Signature("0d0e0f")
    private val callers = CertificateCallers(packages, mapOf(CAR to listOf("00".repeat(32), sha256(carKey))))

    @Test
    fun `a listed package signed with a listed certificate is known`() {
        install(CAR, CAR_UID, carKey)

        assertTrue(callers.isKnown(CAR, CAR_UID))
    }

    @Test
    fun `a listed package signed with another certificate is not known`() {
        install(CAR, CAR_UID, otherKey)

        assertFalse(callers.isKnown(CAR, CAR_UID))
    }

    @Test
    fun `a listed package whose key was changed is known by its older, listed key`() {
        install(CAR, CAR_UID, otherKey, olderKeys = listOf(carKey))

        assertTrue(callers.isKnown(CAR, CAR_UID))
    }

    @Test
    fun `a listed package signed with a listed and an unlisted key at once is not known`() {
        install(CAR, CAR_UID, carKey, otherKey)

        assertFalse(callers.isKnown(CAR, CAR_UID))
    }

    @Test
    fun `an app that only gives the name of a listed package is not known`() {
        install(CAR, CAR_UID, carKey)
        install("other.app", OTHER_UID, otherKey)

        assertFalse(callers.isKnown(CAR, OTHER_UID))
    }

    @Test
    fun `a package that is not listed is not known, whatever it is signed with`() {
        install("other.app", OTHER_UID, carKey)

        assertFalse(callers.isKnown("other.app", OTHER_UID))
    }

    @Test
    fun `a listed package that is not installed is not known`() {
        assertFalse(callers.isKnown(CAR, CAR_UID))
    }

    @Test
    fun `every certificate in the list of the app is a SHA-256 in lower case hex`() {
        val values = CarCallerCertificates.values.flatten()

        assertTrue(values.isNotEmpty())
        assertTrue(values.all { Regex("[0-9a-f]{64}").matches(it) })
    }

    private fun install(
        packageName: String,
        uid: Int,
        vararg keys: Signature,
        olderKeys: List<Signature> = emptyList(),
    ) {
        val signing: SigningInfo = Shadow.newInstanceOf(SigningInfo::class.java)
        Shadow.extract<ShadowSigningInfo>(signing).apply {
            setSignatures(keys)
            if (olderKeys.isNotEmpty()) setPastSigningCertificates((olderKeys + keys).toTypedArray())
        }
        val info = PackageInfo().apply {
            this.packageName = packageName
            signingInfo = signing
        }
        shadowOf(packages).installPackage(info)
        shadowOf(packages).setPackagesForUid(uid, packageName)
    }

    private fun sha256(key: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
}

private const val CAR = "the.car"
private const val CAR_UID = 10_070
private const val OTHER_UID = 10_071
