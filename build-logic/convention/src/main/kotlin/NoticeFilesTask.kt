/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes the `NOTICE` files of the libraries that ship in the app into one asset. */
abstract class NoticeFilesTask : DefaultTask() {
    /** The files of the libraries, as they were downloaded. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val libraries: ConfigurableFileCollection

    /** The `group:name` of the library each file belongs to, by the path of the file. */
    @get:Internal
    abstract val names: MapProperty<String, String>

    @get:OutputDirectory
    abstract val assets: DirectoryProperty

    @TaskAction
    fun collect() {
        val names = names.get()
        val notices = libraries.files.mapNotNull { file ->
            val name = names[file.absolutePath]
            val notice = NoticeFiles.noticeIn(file)
            if (name != null && notice != null) name to notice else null
        }.toMap()
        val asset = assets.get().file(NoticeFiles.ASSET).asFile
        asset.parentFile.mkdirs()
        asset.writeText(NoticeFiles.toJson(notices))
    }
}
