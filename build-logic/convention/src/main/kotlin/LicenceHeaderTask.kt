/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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

/** Fails when a source file of the public repo does not start with the MPL-2.0 notice. */
abstract class LicenceHeaderTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val baseDir: DirectoryProperty

    @TaskAction
    fun check() {
        val base = baseDir.get().asFile
        val problems = sources.files.sorted()
            .filterNot { file -> LicenceHeader.isPresent(file.readText()) }
            .map { file -> file.relativeTo(base).path }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "These files do not start with the MPL-2.0 header (RULES.md K14). " +
                    "Copy it from any other source file:\n" + problems.joinToString("\n") { "  - $it" },
            )
        }
    }
}
