/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// Plugins of the build logic's own build file. The plugin portal serves only the groups that are published
// nowhere else; everything they bring with them comes from Maven Central.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal {
            content {
                includeGroupByRegex("org\\.gradle\\.kotlin.*")
                includeGroupByRegex("org\\.jlleitschuh\\.gradle.*")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // The ktlint plugin is published only on the plugin portal.
        gradlePluginPortal {
            content {
                includeGroup("org.jlleitschuh.gradle")
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
