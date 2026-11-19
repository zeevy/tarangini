/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.gradle.api.JavaVersion

/** Build settings shared by every module. */
object SwaraTarangBuild {
    const val BASE_PACKAGE = "com.siliconcircuits.swaratarang"
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 31
    const val JDK = 21
    val javaVersion: JavaVersion = JavaVersion.toVersion(JDK)

    /** The Android namespace of a module: `:feature:library` gives `<base>.feature.library`, `:app` gives `<base>`. */
    fun namespaceFor(projectPath: String): String =
        if (projectPath == ":app") BASE_PACKAGE else BASE_PACKAGE + projectPath.replace(':', '.')
}
