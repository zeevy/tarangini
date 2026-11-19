/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// A feature module: screens and ViewModels. It sees only the domain and the shared UI.
plugins {
    id("swaratarang.android.library")
    id("swaratarang.android.compose")
}

dependencies {
    "implementation"(project(":core:domain"))
    "implementation"(project(":core:ui"))
    "testImplementation"(project(":core:testing"))
}
