/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.playlist

import java.net.URLDecoder

/**
 * One song as a playlist file names it.
 *
 * @property path where the file is, as other players write it (`/storage/emulated/0/Music/a.mp3`)
 */
data class M3uSong(val path: String, val title: String, val artist: String, val durationMs: Long)

/**
 * The file of a library song, to compare with a line of a playlist file.
 *
 * @property folderPath the folder as the library saves it, for example `external_primary/Music/Rock`
 */
data class SongFile(val songId: Long, val folderPath: String, val fileName: String)

/**
 * Reads and writes `.m3u` playlist files.
 *
 * A file that is read comes from anywhere, so nothing in it is trusted: it has a size limit, a line
 * limit and a limit of lines, and a line is only ever compared with the files the library knows.
 */
object M3u {
    /** A file larger than this is refused. A playlist of 5000 songs is far below it. */
    const val MAX_BYTES = 1024 * 1024

    /** A longer line is no path and is left out. */
    const val MAX_LINE = 4096

    /** The lines after this many are left out. */
    const val MAX_ENTRIES = 5000

    /** The text of a playlist file for [songs], in the extended form most players read. */
    fun write(songs: List<M3uSong>): String = buildString {
        append("#EXTM3U\n")
        songs.forEach { song ->
            val seconds = song.durationMs / MS_PER_SECOND
            append("#EXTINF:$seconds,${oneLine(song.artist)} - ${oneLine(song.title)}\n")
            append(oneLine(song.path)).append('\n')
        }
    }

    /** The lines of [text] that name a file or an address: no comment, no empty line, at most [MAX_ENTRIES]. */
    fun entries(text: String): List<String> = text.removePrefix(BYTE_ORDER_MARK)
        .lineSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.length <= MAX_LINE }
        .take(MAX_ENTRIES)
        .toList()

    /** The file name a line ends with, in lower case, or null for an address on the internet. */
    fun fileNameOf(entry: String): String? = parts(entry).lastOrNull()

    /**
     * The song of [files] that [entry] means, or null.
     *
     * The file whose path has the most folders in common with the line, counted from the file name
     * up, wins. So a path of another computer still matches by its last folders, and so does a
     * relative path. When only the file name is the same, it must be the one file with that name:
     * a guess between two songs would put a wrong song into the playlist.
     */
    fun match(entry: String, files: List<SongFile>): Long? {
        val wanted = parts(entry)
        val scored = files.map { it.songId to commonEnd(wanted, parts("${it.folderPath}/${it.fileName}")) }
        val best = scored.maxOfOrNull { it.second } ?: 0
        val top = scored.filter { it.second == best }.map { it.first }
        val isSure = best > 1 || (best == 1 && top.size == 1)
        return top.minOrNull().takeIf { isSure }
    }

    /** The folders and the file name of a line in lower case, whatever way the line writes its path. */
    private fun parts(entry: String): List<String> {
        val isAddress = STREAM_STARTS.any { entry.startsWith(it, ignoreCase = true) }
        val plain = if (entry.startsWith(
                FILE_START,
                ignoreCase = true,
            )
        ) {
            decoded(entry.substring(FILE_START.length))
        } else {
            entry
        }
        val parts = ArrayList<String>()
        if (!isAddress) {
            plain.replace('\\', '/').split('/').forEach { part ->
                when (part) {
                    "", "." -> Unit
                    ".." -> parts.removeLastOrNull()
                    else -> parts.add(part.lowercase())
                }
            }
        }
        return parts
    }

    /** A `file:` address writes a space and other characters as `%20`. A text that is no such form stays as it is. */
    private fun decoded(text: String): String =
        runCatching { URLDecoder.decode(text.replace("+", "%2B"), "UTF-8") }.getOrDefault(text)

    private fun commonEnd(a: List<String>, b: List<String>): Int =
        a.asReversed().zip(b.asReversed()).takeWhile { (x, y) -> x == y }.size

    /** A title with a line break would start a new line of the file. */
    private fun oneLine(text: String): String = text.replace('\r', ' ').replace('\n', ' ')

    private const val MS_PER_SECOND = 1000
    private const val BYTE_ORDER_MARK = "\uFEFF"
    private const val FILE_START = "file:"
    private val STREAM_STARTS = listOf("http://", "https://")
}
