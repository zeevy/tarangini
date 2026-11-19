/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("swaratarang.android.library")
    id("swaratarang.android.hilt")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.media3.exoplayer)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.media3.session)

    testImplementation(libs.kotlinx.coroutines.test)
}
