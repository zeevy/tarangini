/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
