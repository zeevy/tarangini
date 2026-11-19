/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("com.android.library")
}

android {
    namespace = SwaraTarangBuild.namespaceFor(path)
    compileSdk = SwaraTarangBuild.COMPILE_SDK

    defaultConfig {
        minSdk = SwaraTarangBuild.MIN_SDK
    }

    compileOptions {
        sourceCompatibility = SwaraTarangBuild.javaVersion
        targetCompatibility = SwaraTarangBuild.javaVersion
    }

    // Compose preview tasks need the compiled resources in unit tests.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "testImplementation"(libs.findLibrary("junit").get())
}
