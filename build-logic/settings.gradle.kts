/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
