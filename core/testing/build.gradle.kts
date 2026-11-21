/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
    id("tarangini.android.library")
}

dependencies {
    api(project(":core:domain"))
    // Test rules: the main dispatcher, the plain activity for Compose tests.
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    implementation(libs.robolectric)
}
