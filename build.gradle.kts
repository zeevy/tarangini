/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("swaratarang.module-rules")
}

// The build logic is an included build, so its tests need a hook to run with `./gradlew test`.
tasks.register("test") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
    dependsOn("checkPremiumBoundary")
}
