/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
    id("tarangini.android.application")
    id("tarangini.android.compose")
    id("tarangini.android.hilt")
}

// core: this repo alone, a complete free app with no closed source library.
// play: core plus the private premium module, the Play Store build.
val hasPremium = findProject(":premium") != null
if (!hasPremium) {
    logger.lifecycle("The play build is not configured: the private premium module is not in premium/.")
}

// The one place of the version. A release is the tag v<this version>; the release workflow checks it.
val appVersion = "0.1.0"

android {
    defaultConfig {
        versionCode = TaranginiBuild.versionCodeOf(appVersion)
        versionName = appVersion
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("core")
        if (hasPremium) {
            create("play")
        }
    }
}

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":feature:library"))
    implementation(project(":feature:player"))
    implementation(project(":feature:search"))
    implementation(project(":feature:settings"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:playback"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime)

    // Finds memory leaks while a debug build is used. It is never part of a release build.
    debugImplementation(libs.leakcanary.android)
    if (hasPremium) {
        "playImplementation"(project(":premium"))
    }
}
