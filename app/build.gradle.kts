/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("swaratarang.android.application")
    id("swaratarang.android.compose")
    id("swaratarang.android.hilt")
}

// core: this repo alone, a complete free app with no closed source library.
// play: core plus the private premium module, the Play Store build.
val hasPremium = findProject(":premium") != null
if (!hasPremium) {
    logger.lifecycle("The play build is not configured: the private premium module is not in premium/.")
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "0.1.0"
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
    implementation(project(":feature:radio"))
    implementation(project(":feature:player"))
    implementation(project(":feature:search"))
    implementation(project(":feature:settings"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:playback"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    if (hasPremium) {
        "playImplementation"(project(":premium"))
    }
}
