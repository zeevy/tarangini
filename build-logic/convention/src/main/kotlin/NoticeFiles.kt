/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

import groovy.json.JsonOutput
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Finds the `NOTICE` file a library ships. The Apache licence asks that its content goes with
 * the app, so the build collects these files and the licences screen shows them.
 */
object NoticeFiles {
    /** The asset the notices are written to, read by the licences screen. */
    const val ASSET = "licences/notices.json"

    /** A longer notice is cut here. No notice of a library comes near this size. */
    const val MAX_CHARS = 64 * 1024

    private val NAMES = setOf("NOTICE", "NOTICE.TXT", "NOTICE.MD")
    private const val CLASSES_OF_AAR = "classes.jar"

    /** The notice inside a `.jar` or `.aar` file, or null when it has none. */
    fun noticeIn(library: File): String? = library.inputStream().use(::noticeIn)

    /** The list as the text of the asset: one entry per library, in a fixed order. */
    fun toJson(notices: Map<String, String>): String = JsonOutput.toJson(
        notices.toSortedMap().map { (library, notice) ->
            mapOf(
                "groupId" to library.substringBefore(':'),
                "artifactId" to library.substringAfter(':'),
                "notice" to notice,
            )
        },
    )

    private fun noticeIn(archive: InputStream): String? {
        val zip = ZipInputStream(archive)
        var found: String? = null
        var entry = zip.nextEntry
        while (entry != null && found == null) {
            found = when {
                entry.isDirectory -> null

                // The code of an Android library is a jar inside its file.
                entry.name == CLASSES_OF_AAR -> noticeIn(zip)

                isNotice(entry.name) -> zip.reader().readText().trim().take(MAX_CHARS).ifEmpty { null }

                else -> null
            }
            entry = zip.nextEntry
        }
        return found
    }

    /** True for a notice at the top of the file or in its `META-INF` folder. */
    private fun isNotice(path: String): Boolean {
        val folder = path.substringBeforeLast('/', "")
        return (folder.isEmpty() || folder == "META-INF") && path.substringAfterLast('/').uppercase() in NAMES
    }
}
