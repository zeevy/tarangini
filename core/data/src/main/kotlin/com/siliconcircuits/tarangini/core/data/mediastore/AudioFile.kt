/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

/**
 * One audio file as the phone describes it, before it is saved as a song.
 *
 * @property id the id the phone gives the file; it stays the same while the file stays in place
 * @property folderPath the storage name and the folders the file is in, joined
 *   with "/", for example `external_primary/Music/Rock`. It has no "/" at the ends
 * @property title the title tag, or null when the file has none
 * @property artist the artist of the song, or null when the file has no tag
 * @property albumArtist the artist of the whole album, or null when the file has no tag
 * @property durationMs the length, or null when the phone does not know it
 * @property dateAdded when the file came to the phone, in milliseconds since 1970 UTC
 * @property genre the genre tag as the file states it; it can name more than one genre
 * @property dateModified when the file last changed, in milliseconds since 1970 UTC
 */
internal data class AudioFile(
    val id: String,
    val fileName: String,
    val folderPath: String,
    val mimeType: String,
    val title: String?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val durationMs: Long?,
    val sizeBytes: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val genre: String? = null,
    val dateAdded: Long,
    val dateModified: Long,
)
