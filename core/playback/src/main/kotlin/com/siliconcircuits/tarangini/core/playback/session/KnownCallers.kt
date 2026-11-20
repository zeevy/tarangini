/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.content.pm.PackageManager
import android.content.pm.Signature
import java.security.MessageDigest

/** Says whether a caller is one of the apps that are let in by name and signing certificate. */
internal fun interface KnownCallers {
    fun isKnown(packageName: String, uid: Int): Boolean
}

/**
 * The apps of Google that show this player in the car and play by voice,
 * each with the SHA-256 of the certificates it is signed with.
 *
 * The values are the ones Google publishes in the caller list of its sample
 * player (github.com/android/uamp, `allowed_media_browser_callers.xml`). The
 * second value of the Google app is the certificate it is signed with on
 * Android 13 and newer; it was read from the app on a phone.
 */
internal val CarCallerCertificates: Map<String, List<String>> = mapOf(
    "com.google.android.projection.gearhead" to listOf(
        "fdb00c43dbde8b51cb312aa81d3b5fa17713adb94b28f598d77f8eb89daceedf",
        "1ca8dcc0bed3cbd872d2cb791200c0292ca9975768a82d676b8b424fb65b5295",
    ),
    "com.google.android.googlequicksearchbox" to listOf(
        "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83",
        "7ce83c1b71f3d572fed04c8d40c5cb10ff75e6d87d9df6fbd53f0468c2905053",
    ),
)

/**
 * Lets a caller in when its package is in [certificates], the package really
 * belongs to the calling user id, and the system says it is signed with one
 * of the listed certificates.
 *
 * A package name alone can be claimed by any app. The user id comes from
 * the system and cannot be faked, so the name counts only when the system
 * says that this user id owns the package. The certificates are then read
 * from the installed package, not from anything the caller sends.
 */
internal class CertificateCallers(
    private val packages: PackageManager,
    private val certificates: Map<String, List<String>> = CarCallerCertificates,
) : KnownCallers {
    override fun isKnown(packageName: String, uid: Int): Boolean {
        val allowed = certificates[packageName].orEmpty()
        val ownsPackage = packages.getPackagesForUid(uid)?.contains(packageName) == true
        return allowed.isNotEmpty() && ownsPackage && isSignedWith(packageName, allowed)
    }

    private fun isSignedWith(packageName: String, allowed: List<String>): Boolean {
        val signing = try {
            packages.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        return when {
            signing == null -> false

            // Signed by several keys at once: every one of them must be a listed one.
            signing.hasMultipleSigners() -> signing.apkContentsSigners.orEmpty().allIn(allowed)

            // One key. An app whose key was changed lists the older ones too, as the Google app does,
            // and the system accepts such an app as the same app. One listed key in that line is enough.
            else -> signing.signingCertificateHistory.orEmpty().any { it.toByteArray().sha256() in allowed }
        }
    }
}

private fun Array<out Signature>.allIn(allowed: List<String>): Boolean =
    isNotEmpty() && all { it.toByteArray().sha256() in allowed }

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }
