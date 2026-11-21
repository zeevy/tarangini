/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
    id("tarangini.android.feature")
    id("tarangini.android.hilt")
}

dependencies {
    implementation(libs.androidx.glance.appwidget)
    // Only for the media button receiver and the action of the playback service, which the buttons name.
    implementation(libs.media3.session)

    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
