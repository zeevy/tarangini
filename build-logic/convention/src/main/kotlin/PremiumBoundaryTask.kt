/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Fails when a source, XML or ProGuard file of the public modules names the premium package. */
abstract class PremiumBoundaryTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val baseDir: DirectoryProperty

    @TaskAction
    fun check() {
        val base = baseDir.get().asFile
        val problems = sources.files.sorted().flatMap { file ->
            PremiumBoundary.violations(file.readText()).map { line -> "${file.relativeTo(base)}:$line" }
        }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Public code names the premium package ${PremiumBoundary.PREMIUM_PACKAGE} (RULES.md H2):\n" +
                    problems.joinToString("\n") { "  - $it" },
            )
        }
    }
}
