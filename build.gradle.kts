/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    id("swaratarang.module-rules")
    id("swaratarang.quality")
}

// The build logic is an included build, so its tests need a hook to run with `./gradlew test`.
tasks.register("test") {
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
    dependsOn("checkPremiumBoundary")
    dependsOn("checkLicenceHeaders")
}

// The same hook for the style and code checks of the build logic.
listOf("ktlintCheck", "detekt").forEach { check ->
    tasks.named(check) {
        dependsOn(gradle.includedBuild("build-logic").task(":convention:$check"))
    }
}
