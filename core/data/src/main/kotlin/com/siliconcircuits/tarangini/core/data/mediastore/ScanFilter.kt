/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

/**
 * Says which files belong in the library: only the formats the app plays,
 * and only what the settings of the user allow.
 *
 * @property minTrackMs files shorter than this are left out; a file with no known length stays
 * @property includedFolders when not empty, only files in these folders, or in folders inside them, stay
 * @property excludedFolders files in these folders, or in folders inside them, are left out
 */
internal data class ScanFilter(
    val minTrackMs: Long = 0,
    val includedFolders: Set<String> = emptySet(),
    val excludedFolders: Set<String> = emptySet(),
) {
    private val included = includedFolders.mapNotNull(::cleanFolder)
    private val excluded = excludedFolders.mapNotNull(::cleanFolder)

    fun accepts(file: AudioFile): Boolean = hasPlayableFormat(file) && !leavesOut(file)

    /** True for a file the app can play that the settings of the user leave out. */
    fun hides(file: AudioFile): Boolean = hasPlayableFormat(file) && leavesOut(file)

    private fun leavesOut(file: AudioFile): Boolean = (file.durationMs != null && file.durationMs < minTrackMs) ||
        (included.isNotEmpty() && included.none { file.folderPath.isInside(it) }) ||
        excluded.any { file.folderPath.isInside(it) }

    private fun hasPlayableFormat(file: AudioFile): Boolean =
        FORMATS.any { file.fileName.endsWith(it, ignoreCase = true) }

    /** A saved folder with no "/" at the ends, or null for an entry that names no folder. */
    private fun cleanFolder(folder: String): String? = folder.trim().trim('/').takeIf { it.isNotEmpty() }

    /** True for the folder itself and for every folder below it, but not for "Music2" when the folder is "Music". */
    private fun String.isInside(folder: String): Boolean = this == folder || startsWith("$folder/")

    private companion object {
        val FORMATS = listOf(".mp3", ".aac", ".m4a")
    }
}
