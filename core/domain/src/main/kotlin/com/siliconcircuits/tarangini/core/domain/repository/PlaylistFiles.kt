/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

/** Moves playlists in and out of the app as `.m3u` files. */
interface PlaylistFiles {
    /**
     * Writes the playlist to the file the person picked.
     *
     * @param target the address the file picker of the system gave
     * @return false when the file could not be written
     */
    suspend fun exportTo(playlistId: Long, target: String): Boolean

    /**
     * Makes a new playlist from the file the person picked. The file is not trusted.
     *
     * @param source the address the file picker of the system gave
     */
    suspend fun importFrom(source: String): PlaylistImport
}

/** How the import of a playlist file ended. */
sealed interface PlaylistImport {
    /**
     * A playlist was made.
     *
     * @property matched how many lines of the file are songs of the library
     * @property total how many lines the file had
     * @property notFound the first lines with no song in the library, as the file wrote them
     */
    data class Done(
        val playlistId: Long,
        val name: String,
        val matched: Int,
        val total: Int,
        val notFound: List<String>,
    ) : PlaylistImport

    /** The file is larger than a playlist file can be. Nothing was read. */
    data object TooLarge : PlaylistImport

    /** No line of the file is a song of the library, or it is no playlist file. No playlist was made. */
    data object NothingFound : PlaylistImport

    /** The file could not be read. */
    data object Failed : PlaylistImport

    companion object {
        /** At most this many lines are kept in [Done.notFound]. */
        const val MAX_NOT_FOUND = 50
    }
}
