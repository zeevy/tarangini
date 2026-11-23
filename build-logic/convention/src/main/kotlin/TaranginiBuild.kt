/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import org.gradle.api.JavaVersion

/** Build settings shared by every module. */
object TaranginiBuild {
    const val BASE_PACKAGE = "com.siliconcircuits.tarangini"
    const val COMPILE_SDK = 37

    // Compose 1.13 compiles only against API level 37.1 or later.
    const val COMPILE_SDK_MINOR = 1
    const val TARGET_SDK = 37
    const val MIN_SDK = 31
    const val JDK = 21
    val javaVersion: JavaVersion = JavaVersion.toVersion(JDK)

    /** The Android namespace of a module: `:feature:library` gives `<base>.feature.library`, `:app` gives `<base>`. */
    fun namespaceFor(projectPath: String): String =
        if (projectPath == ":app") BASE_PACKAGE else BASE_PACKAGE + projectPath.replace(':', '.')

    /**
     * The version code of a version name of the form `major.minor.patch`: 1.4.2 gives 1004002.
     * The code then grows with every release, which is what Android and the Play Store ask for.
     * Minor and patch go up to 999. Any other form of name fails the build.
     */
    fun versionCodeOf(versionName: String): Int {
        // Plain numbers only, with no sign and no leading zero, so one version has one way to write it.
        val parts = versionName.split('.').map { part -> part.takeIf(PART::matches)?.toIntOrNull() }
        require(parts.size == VERSION_PARTS && parts.all { it != null }) {
            "The version name must be major.minor.patch, but is '$versionName'"
        }
        val (major, minor, patch) = parts.map { it ?: 0 }
        require(minor <= MAX_PART && patch <= MAX_PART && major <= MAX_MAJOR) {
            "A part of the version '$versionName' is too large"
        }
        return (major * PART_STEP + minor) * PART_STEP + patch
    }

    private val PART = Regex("0|[1-9][0-9]{0,3}")
    private const val VERSION_PARTS = 3
    private const val MAX_PART = 999
    private const val MAX_MAJOR = 2000
    private const val PART_STEP = 1000
}
