/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
    id("com.android.library")
    id("tarangini.quality")
}

android {
    namespace = TaranginiBuild.namespaceFor(path)
    compileSdk = TaranginiBuild.COMPILE_SDK

    defaultConfig {
        minSdk = TaranginiBuild.MIN_SDK
    }

    compileOptions {
        sourceCompatibility = TaranginiBuild.javaVersion
        targetCompatibility = TaranginiBuild.javaVersion
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
