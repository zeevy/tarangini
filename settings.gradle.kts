/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // The ktlint plugin is published only on the plugin portal.
        gradlePluginPortal {
            content {
                includeGroupByRegex("org\\.jlleitschuh\\.gradle.*")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tarangini"

include(":app")
include(":baselineprofile")
include(":feature:home")
include(":feature:library")
include(":feature:radio")
include(":feature:player")
include(":feature:search")
include(":feature:settings")
include(":feature:widget")
include(":core:domain")
include(":core:data")
include(":core:playback")
include(":core:ui")
include(":core:testing")

// The private premium module is not part of this repo. The play build exists only when it is checked out here.
if (file("premium/build.gradle.kts").exists()) {
    include(":premium")
}
