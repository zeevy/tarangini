/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

plugins {
    id("tarangini.module-rules")
    id("tarangini.quality")
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
