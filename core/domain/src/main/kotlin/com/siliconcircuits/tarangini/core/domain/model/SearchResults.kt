/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * What a search of the library found, in three groups. Each group has the
 * best match first.
 *
 * @property songs songs with every word of the search in their title, artist or album
 * @property albums albums with every word in their title
 * @property artists artists with every word in their name
 */
data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
) {
    /** True when nothing was found. */
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}
