/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("com.android.library")
    id("swaratarang.quality")
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

    lint {
        lintConfig = rootProject.file("config/lint.xml")
    }

    // Compose preview tasks need the compiled resources in unit tests.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // One Robolectric settings file for every module.
    sourceSets.getByName("test").resources.srcDir(rootProject.file("config/robolectric"))
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "testImplementation"(libs.findLibrary("junit").get())
    "testImplementation"(libs.findLibrary("robolectric").get())
}
