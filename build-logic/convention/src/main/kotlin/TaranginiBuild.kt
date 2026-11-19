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
    const val TARGET_SDK = 37
    const val MIN_SDK = 31
    const val JDK = 21
    val javaVersion: JavaVersion = JavaVersion.toVersion(JDK)

    /** The Android namespace of a module: `:feature:library` gives `<base>.feature.library`, `:app` gives `<base>`. */
    fun namespaceFor(projectPath: String): String =
        if (projectPath == ":app") BASE_PACKAGE else BASE_PACKAGE + projectPath.replace(':', '.')
}
