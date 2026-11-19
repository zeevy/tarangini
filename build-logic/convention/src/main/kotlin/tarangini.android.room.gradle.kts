/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

// Room for an Android module: the plugin, the library, the annotation processor and the test helper.
plugins {
    id("com.google.devtools.ksp")
    id("androidx.room3")
}

// The schema of every database version is written here and committed, so migrations can be tested.
room3 {
    schemaDirectory("$projectDir/schemas")
}

// The migration tests read the committed schema files as test assets.
pluginManager.withPlugin("com.android.library") {
    extensions.configure<com.android.build.api.dsl.LibraryExtension> {
        sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(libs.findLibrary("room-runtime").get())
    "implementation"(libs.findLibrary("androidx-sqlite-framework").get())
    "ksp"(libs.findLibrary("room-compiler").get())
    "testImplementation"(libs.findLibrary("room-testing").get())
}
