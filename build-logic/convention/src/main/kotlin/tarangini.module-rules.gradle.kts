/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.gradle.api.artifacts.ProjectDependency

// Fails every build when a module breaks the dependency table.
gradle.projectsEvaluated {
    val problems = rootProject.allprojects.flatMap { module ->
        module.configurations.flatMap { configuration ->
            configuration.dependencies.withType(ProjectDependency::class.java).mapNotNull { dependency ->
                ModuleRules.violation(module.path, dependency.path, configuration.name)
            }
        }
    }.distinct()
    if (problems.isNotEmpty()) {
        throw GradleException(
            "Module dependency rules are broken (RULES.md A2):\n" + problems.joinToString("\n") { "  - $it" },
        )
    }
}

tasks.register<PremiumBoundaryTask>("checkPremiumBoundary") {
    group = "verification"
    description = "Fails when a source, XML or ProGuard file of the public modules names the premium package."
    baseDir.set(layout.projectDirectory)
    sources.from(
        listOf("app", "core", "feature").map { folder ->
            fileTree(folder) {
                include("**/src/**/*.kt", "**/src/**/*.java", "**/src/**/*.xml", "**/*.pro")
                exclude("build/**", "*/build/**")
            }
        },
    )
}

tasks.register<LicenceHeaderTask>("checkLicenceHeaders") {
    group = "verification"
    description = "Fails when a Kotlin, Java, Gradle or XML source file does not start with the MPL-2.0 header."
    baseDir.set(layout.projectDirectory)
    sources.from(fileTree(layout.projectDirectory) { include("*.kts") })
    sources.from(
        listOf("app", "core", "feature", "build-logic").map { folder ->
            fileTree(folder) {
                include("**/*.kts", "**/src/**/*.kt", "**/src/**/*.java", "**/src/**/*.xml")
                exclude("build/**", "*/build/**", ".gradle/**", "*/.gradle/**")
            }
        },
    )
}
