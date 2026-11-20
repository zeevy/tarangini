/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.stream

import okhttp3.HttpUrl

/**
 * True when the address names a playlist file by its ending: `.pls` or `.m3u`. Such a file is not
 * a stream. It is a short text that lists where the stream is. An `.m3u8` address is HLS, which the
 * player reads by itself.
 */
internal fun HttpUrl.isPlaylistFile(): Boolean {
    val name = pathSegments.lastOrNull().orEmpty().lowercase()
    return name.endsWith(".pls") || name.endsWith(".m3u")
}

/**
 * The addresses that the text of a playlist file lists, in their order. A `.pls` file has lines
 * such as `File1=address` among other lines with a `=`. An `.m3u` file has one address per line and
 * comments that start with `#`. A text with the `[playlist]` header or a `File` line is read as the
 * first kind, any other text as the second. The addresses are not checked here.
 */
internal fun playlistEntries(text: String): List<String> {
    val lines = text.removePrefix(BYTE_ORDER_MARK)
        .lineSequence()
        .map { it.trim() }
        .filterNot { it.isEmpty() || it.startsWith("#") }
        .toList()
    val fileLines = lines.filter { plsAddress.matches(it.substringBefore('=', missingDelimiterValue = "").trim()) }
    val isPls = fileLines.isNotEmpty() || lines.any { it.equals(PLS_HEADER, ignoreCase = true) }
    val entries = if (isPls) fileLines.map { it.substringAfter('=').trim() } else lines
    return entries.filter { it.isNotEmpty() }
}

private const val PLS_HEADER = "[playlist]"
private const val BYTE_ORDER_MARK = "\uFEFF"
private val plsAddress = Regex("file\\d+", RegexOption.IGNORE_CASE)
