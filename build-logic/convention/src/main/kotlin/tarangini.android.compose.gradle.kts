/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension

// Compose for an Android module, with what its screen and screenshot tests need.
plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

pluginManager.withPlugin("com.android.library") {
    extensions.configure<LibraryExtension> { buildFeatures { compose = true } }
}
pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> { buildFeatures { compose = true } }
}

// Screenshot tests compare with the saved pictures. -PrecordScreenshots saves new pictures instead.
val recordScreenshots = providers.gradleProperty("recordScreenshots").map { it != "false" }.getOrElse(false)
tasks.withType<Test>().configureEach {
    // The tests read the saved pictures, so a changed picture must run them again.
    inputs.files(fileTree("src/test/screenshots")).withPropertyName("savedScreenshots")
    systemProperty("roborazzi.test.record", recordScreenshots)
    systemProperty("roborazzi.test.verify", !recordScreenshots)
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(platform(libs.findLibrary("androidx-compose-bom").get()))
    "implementation"(libs.findLibrary("androidx-compose-material3").get())
    "implementation"(libs.findLibrary("androidx-compose-ui").get())
    "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
    "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
    "testImplementation"(libs.findLibrary("androidx-compose-ui-test-junit4").get())
    "testImplementation"(libs.findLibrary("roborazzi").get())
    "testImplementation"(libs.findLibrary("roborazzi-compose").get())
}
