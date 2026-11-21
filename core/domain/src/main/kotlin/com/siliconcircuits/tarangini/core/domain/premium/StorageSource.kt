/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.premium

import java.io.InputStream

/**
 * A place where audio files live: the media of the phone, or later a cloud storage of the person
 * (WebDAV, S3, OneDrive) from the premium module. The app gets every source as a Hilt set; the
 * core build has only the source of the phone, `local`.
 *
 * A path is the source's own text for a file or a folder. Only the source that gave it reads it.
 */
interface StorageSource {
    /** A short name that tells the sources apart, such as `local`. It never changes. */
    val id: String

    /**
     * The files and folders right inside [folder], folders first, then files, each by name. The
     * empty path is the top. A folder the source does not know has nothing in it. The read can
     * take long; it does not block the caller's thread.
     */
    suspend fun list(folder: String = ""): List<StorageEntry>

    /**
     * Opens the file at [path] from the first byte of [range], to read at most its length. The
     * caller closes the stream. This blocks: call it on a background thread, as a player does.
     *
     * @throws java.io.FileNotFoundException when the file is gone or cannot be opened
     */
    fun open(path: String, range: ByteRange = ByteRange()): InputStream
}

/**
 * A file or a folder of a [StorageSource].
 *
 * @property path what [StorageSource.open] or [StorageSource.list] takes for it
 * @property name the name to show
 * @property sizeBytes the size of a file, or null for a folder or when it is not known
 */
data class StorageEntry(val path: String, val name: String, val isFolder: Boolean, val sizeBytes: Long? = null)

/**
 * The part of a file to read: from [offset], [length] bytes, or to the end when [length] is null.
 */
data class ByteRange(val offset: Long = 0, val length: Long? = null) {
    init {
        require(offset >= 0) { "The offset of a range is 0 or more, not $offset" }
        require(length == null || length >= 0) { "The length of a range is 0 or more, not $length" }
    }
}
