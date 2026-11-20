/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * One song of the library.
 *
 * @property id the id of the song in the library of the app. It stays the same when the file is tagged again
 * @property mediaUri the address the player plays. It is also the id of the song in the queue
 * @property artist the artist name, or an empty text when the file names none
 * @property album the album title, or an empty text when the file names none
 * @property artworkUri where to ask for the cover picture (see [artworkUriOf]), or null when there is no such place
 * @property dateAdded when the file came onto the phone, in milliseconds since 1970 UTC
 */
data class Song(
    val id: Long,
    val mediaUri: String,
    val title: String,
    val artistId: Long,
    val artist: String,
    val albumId: Long,
    val album: String,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val dateAdded: Long,
) {
    val artworkUri: String? get() = artworkUriOf(mediaUri)
}
