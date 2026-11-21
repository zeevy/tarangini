/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** An audio file with tags, in the folder `external_primary/Music`. A test sets only what it is about. */
@Suppress("LongParameterList") // Each test sets only the values it is about.
internal fun audioFile(
    id: String,
    title: String? = "Song $id",
    fileName: String = "song-$id.mp3",
    folderPath: String = "external_primary/Music",
    artist: String? = "Artist",
    albumArtist: String? = null,
    album: String? = "Album",
    durationMs: Long? = 180_000,
    sizeBytes: Long = 1_000 + id.hashCode().toLong(),
    dateModified: Long = 1_000,
    genre: String? = null,
) = AudioFile(
    id = id,
    fileName = fileName,
    folderPath = folderPath,
    mimeType = "audio/mpeg",
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    trackNumber = null,
    discNumber = null,
    year = null,
    genre = genre,
    dateAdded = 500,
    dateModified = dateModified,
)

/** A file source for tests: it gives the list a test puts in [files]. */
internal class FakeAudioFileSource(var files: List<AudioFile>? = emptyList(), var permission: Boolean = true) :
    AudioFileSource {
    /** When set, reading the files throws this, the way a broken read on a phone does. */
    var failure: RuntimeException? = null

    /** A test sends into this to say that the files changed. */
    val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    override fun hasPermission() = permission

    override fun changes(): Flow<Unit> = changed

    override fun files(): List<AudioFile>? {
        failure?.let { throw it }
        return files
    }
}
