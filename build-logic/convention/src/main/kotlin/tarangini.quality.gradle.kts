/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Style (ktlint) and code rules (detekt) for one module. The settings live in
// .editorconfig and config/detekt.yml at the root of the repo.
plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

configure<KtlintExtension> {
    version.set(libs.findVersion("ktlint").get().requiredVersion)
    android.set(true)
}

configure<DetektExtension> {
    config.setFrom(rootProject.file("config/detekt.yml"))
    buildUponDefaultConfig = true
    // Every source set, with the flavour folders such as src/core.
    source.setFrom(files("src"))
}

dependencies {
    "detektPlugins"(libs.findLibrary("compose-rules-detekt").get())
}
