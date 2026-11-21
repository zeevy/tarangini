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

    /** At most this many notices are kept from one file. No library has more than a few. */
    private const val MAX_NOTICES = 20
    private const val NAME_START = "NOTICE"
    private const val JAR_ENDING = ".jar"
    private const val BETWEEN = "\n\n"

    /**
     * The notices inside a `.jar` or `.aar` file, joined, or null when it has none. An Android
     * library keeps its code in jars inside its file, so those are read too.
     */
    fun noticeIn(library: File): String? = library.inputStream().use { noticesIn(it, isInside = false) }
        .distinct()
        .joinToString(BETWEEN)
        .take(MAX_CHARS)
        .ifEmpty { null }

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

    private fun noticesIn(archive: InputStream, isInside: Boolean): List<String> {
        val zip = ZipInputStream(archive)
        val found = mutableListOf<String>()
        var entry = zip.nextEntry
        while (entry != null && found.size < MAX_NOTICES) {
            when {
                entry.isDirectory -> Unit

                // Only one step down: a jar inside a jar inside a file is no library.
                !isInside && entry.name.endsWith(JAR_ENDING) -> found += noticesIn(zip, isInside = true)

                isNotice(entry.name) -> textOf(zip).takeIf { it.isNotEmpty() }?.let(found::add)
            }
            entry = zip.nextEntry
        }
        return found
    }

    /** The start of an entry as text. Never more than [MAX_CHARS] are read, whatever the entry holds. */
    private fun textOf(entry: InputStream): String {
        val reader = entry.reader()
        val text = CharArray(MAX_CHARS)
        var filled = 0
        var read = 0
        while (read >= 0 && filled < text.size) {
            filled += read
            read = reader.read(text, filled, text.size - filled)
        }
        return String(text, 0, filled).trim()
    }

    /** True for a file whose name starts with NOTICE, at the top of the file or in its `META-INF` folder. */
    private fun isNotice(path: String): Boolean {
        val folder = path.substringBeforeLast('/', "")
        return (folder.isEmpty() || folder == "META-INF") &&
            path.substringAfterLast('/').uppercase().startsWith(NAME_START)
    }
}
