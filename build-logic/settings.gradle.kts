/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
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
