/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.playlist

import com.siliconcircuits.tarangini.core.data.local.dao.SongFileRow
import com.siliconcircuits.tarangini.core.domain.playlist.M3u
import com.siliconcircuits.tarangini.core.domain.playlist.SongFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Finds the songs of the library that a file names by their paths: a line of a playlist file or a
 * song of a backup. The rule for a match is [M3u.match].
 *
 * @param filesNamed the files of the library with one of the names given, whatever the case of the
 * letters. The caller decides whether a song whose file is gone for now is part of the answer
 */
internal class SongFileFinder(private val filesNamed: suspend (names: List<String>) -> List<SongFileRow>) {
    /** For each path the song it means, or null when the library has no such song. */
    suspend fun find(paths: List<String>): List<Long?> {
        // The table compares without regard to case only for the letters a to z. So each name is also
        // asked for as the file writes it: "Été.mp3" is then found when the file writes it the same way.
        val names = paths.flatMap { path -> listOfNotNull(M3u.fileNameOf(path), writtenNameOf(path)) }.distinct()
        val files = names.chunked(NAMES_PER_QUERY)
            .flatMap { part ->
                currentCoroutineContext().ensureActive()
                filesNamed(part)
            }
            .distinctBy { it.songId }
            .map { SongFile(it.songId, it.folderPath, it.fileName) }
        val byName = files.groupBy { it.fileName.lowercase() }
        val byPath = files.groupBy { "${it.folderPath}/${it.fileName}".lowercase() }
        return paths.map { path ->
            // A long list can take a while, so a cancelled call ends here.
            currentCoroutineContext().ensureActive()
            // The whole path as the library saves it needs no comparing of folders. A backup read on
            // the phone that wrote it names every song this way.
            byPath[path.lowercase()]?.singleOrNull()?.songId
                ?: M3u.match(path, byName[M3u.fileNameOf(path)].orEmpty())
        }
    }

    /** The file name as the path writes it, for a path with either kind of slash. */
    private fun writtenNameOf(path: String): String = path.replace('\\', '/').substringAfterLast('/')

    private companion object {
        const val NAMES_PER_QUERY = 400
    }
}
