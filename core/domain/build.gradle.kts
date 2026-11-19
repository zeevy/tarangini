/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("tarangini.kotlin.library")
}

dependencies {
    // `PlaybackController` gives its state as a `StateFlow`, so every user of the domain needs the type.
    api(libs.kotlinx.coroutines.core)
}
