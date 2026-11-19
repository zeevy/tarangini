/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

// A feature module: screens and ViewModels. It sees only the domain and the shared UI.
plugins {
    id("tarangini.android.library")
    id("tarangini.android.compose")
    // A feature offers its routes as serializable classes.
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(libs.findLibrary("androidx-navigation-compose").get())
    "implementation"(libs.findLibrary("kotlinx-serialization-core").get())
    "implementation"(project(":core:domain"))
    "implementation"(project(":core:ui"))
    "testImplementation"(project(":core:testing"))
}
