/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.siliconcircuits.tarangini.core.data.di.IoDispatcher
import com.siliconcircuits.tarangini.core.domain.premium.ByteRange
import com.siliconcircuits.tarangini.core.domain.premium.StorageEntry
import com.siliconcircuits.tarangini.core.domain.premium.StorageSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The audio files of the phone as a [StorageSource]. The list comes from the same query of the
 * media index the library scan uses; a folder is a [AudioFile.folderPath] such as
 * `external_primary/Music/Rock`, a file is its id in the media index. Each list reads the whole
 * index once, as a scan does; a screen that opens many folders should keep the result.
 */
internal class LocalStorageSource @Inject constructor(
    private val files: AudioFileSource,
    private val opener: AudioFileOpener,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StorageSource {
    override val id: String = ID

    override suspend fun list(folder: String): List<StorageEntry> = withContext(io) {
        val all = if (files.hasPermission()) files.files().orEmpty() else emptyList()
        val path = folder.trim('/')
        val prefix = if (path.isEmpty()) "" else "$path/"
        val folders = all.asSequence()
            .map { it.folderPath }
            .filter { it.startsWith(prefix) && it.length > prefix.length }
            .map { prefix + it.removePrefix(prefix).substringBefore('/') }
            .distinct()
            .map { StorageEntry(path = it, name = it.substringAfterLast('/'), isFolder = true) }
            .sortedBy { it.name.lowercase() }
        val inside = all.asSequence()
            .filter { it.folderPath == path }
            .map { StorageEntry(path = it.id, name = it.fileName, isFolder = false, sizeBytes = it.sizeBytes) }
            .sortedBy { it.name.lowercase() }
        (folders + inside).toList()
    }

    override fun open(path: String, range: ByteRange): InputStream {
        val id =
            path.toLongOrNull()?.takeIf { it >= 0 }
                ?: throw FileNotFoundException("No file of the phone has the id $path")
        val input = opener.open(id)
        return try {
            input.skipTo(range.offset)
            range.length?.let { LimitedStream(input, it) } ?: input
        } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
            // Any failure here must close the file before it goes on.
            input.close()
            throw failure
        }
    }

    companion object {
        /** The id of the source of the phone. */
        const val ID = "local"
    }
}

/** Opens a file of the media index by its id. */
internal fun interface AudioFileOpener {
    /** @throws FileNotFoundException when the file is gone */
    fun open(id: Long): InputStream
}

/** Opens files through the content resolver, so the app needs no file path. */
internal class ContentAudioFileOpener @Inject constructor(@ApplicationContext private val context: Context) :
    AudioFileOpener {
    override fun open(id: Long): InputStream {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        // Without the audio permission the resolver refuses with a SecurityException; for a caller
        // that is the same as a file it cannot open.
        val file = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (refused: SecurityException) {
            throw FileNotFoundException("No access to $uri").apply { initCause(refused) }
        } ?: throw FileNotFoundException("$uri")
        return ParcelFileDescriptor.AutoCloseInputStream(file)
    }
}

/** Moves to [offset]. A file moves at once; any other stream is read past. */
private fun InputStream.skipTo(offset: Long) {
    if (this is FileInputStream) {
        channel.position(offset)
    } else {
        var left = offset
        while (left > 0) {
            val skipped = skip(left)
            // A stream that skips nothing is read one byte at a time; the end stops the loop.
            left -= when {
                skipped > 0 -> skipped
                read() == -1 -> left
                else -> 1
            }
        }
    }
}

/** Gives at most [length] bytes of [input], then the end. */
private class LimitedStream(input: InputStream, length: Long) : FilterInputStream(input) {
    private var left = length

    override fun read(): Int = if (left <= 0) -1 else super.read().also { if (it >= 0) left-- }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (left <= 0) return -1
        val read = super.read(buffer, offset, minOf(length.toLong(), left).toInt())
        if (read > 0) left -= read
        return read
    }

    override fun skip(count: Long): Long = super.skip(minOf(count, left)).also { left -= it }

    override fun available(): Int = minOf(super.available().toLong(), left).toInt()

    override fun markSupported(): Boolean = false
}
