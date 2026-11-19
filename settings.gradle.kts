/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SwaraTarang"

include(":app")
include(":feature:home")
include(":feature:library")
include(":feature:radio")
include(":feature:player")
include(":feature:search")
include(":feature:settings")
include(":core:domain")
include(":core:data")
include(":core:playback")
include(":core:ui")
include(":core:testing")

// The private premium module is not part of this repo. The play build exists only when it is checked out here.
if (file("premium/build.gradle.kts").exists()) {
    include(":premium")
}
