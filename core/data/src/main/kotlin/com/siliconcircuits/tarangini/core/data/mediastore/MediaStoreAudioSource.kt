/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * Reads the audio files from MediaStore, the media index of the phone. This
 * is the only class that queries MediaStore; every list in the app reads the
 * library that the scanner fills from it.
 */
internal class MediaStoreAudioSource @Inject constructor(@ApplicationContext private val context: Context) :
    AudioFileSource {
    override fun hasPermission(): Boolean {
        // Android 13 split the storage permission; older versions know only the wide one.
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun changes(): Flow<Unit> = callbackFlow {
        // No handler: the phone calls on one of its own threads, and sending into the flow is safe from any thread.
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val resolver = context.contentResolver
        resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate()

    override fun files(): List<AudioFile>? {
        val cursor = try {
            context.contentResolver.query(
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                COLUMNS,
                null,
                null,
                null,
            )
        } catch (_: SecurityException) {
            // The permission was taken away between the check and the query.
            null
        }
        return try {
            cursor?.use { rows ->
                buildList {
                    while (rows.moveToNext()) {
                        add(rows.toAudioFile())
                    }
                }
            }
        } catch (_: IllegalStateException) {
            // A row the phone cannot hand over, for example one larger than its read buffer. A list
            // with rows missing would make their songs look gone, so there is no list at all.
            null
        } catch (_: IllegalArgumentException) {
            // A column this phone does not have.
            null
        }
    }

    private fun Cursor.toAudioFile() = AudioFile(
        id = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media._ID)).toString(),
        fileName = text(MediaStore.Audio.Media.DISPLAY_NAME).orEmpty().take(MAX_FILE_NAME),
        folderPath = folderPath(text(MediaStore.Audio.Media.VOLUME_NAME), text(MediaStore.Audio.Media.RELATIVE_PATH)),
        mimeType = text(MediaStore.Audio.Media.MIME_TYPE).orEmpty(),
        title = text(MediaStore.Audio.Media.TITLE),
        artist = tag(MediaStore.Audio.Media.ARTIST),
        albumArtist = tag(MediaStore.Audio.Media.ALBUM_ARTIST),
        album = tag(MediaStore.Audio.Media.ALBUM),
        durationMs = number(MediaStore.Audio.Media.DURATION)?.takeIf { it > 0 },
        sizeBytes = number(MediaStore.Audio.Media.SIZE) ?: 0,
        trackNumber = leadingNumber(text(MediaStore.Audio.Media.CD_TRACK_NUMBER)),
        discNumber = leadingNumber(text(MediaStore.Audio.Media.DISC_NUMBER)),
        year = number(MediaStore.Audio.Media.YEAR)?.toInt()?.takeIf { it > 0 },
        genre = tag(MediaStore.Audio.Media.GENRE, controlAs = GENRE_DIVIDER),
        dateAdded = (number(MediaStore.Audio.Media.DATE_ADDED) ?: 0) * MS_PER_SECOND,
        dateModified = (number(MediaStore.Audio.Media.DATE_MODIFIED) ?: 0) * MS_PER_SECOND,
    )

    /**
     * A text value, or null when it is missing or blank. Tags and names are
     * written by whoever made the file, so they are cut to [MAX_TEXT] and
     * lose their control characters before anything else sees them.
     */

    /**
     * A text with no control characters. They are left out, or exchanged for [controlAs]: the
     * newer tag format divides the genres of one tag by the zero character.
     */
    private fun Cursor.text(column: String, controlAs: Char? = null): String? {
        val index = getColumnIndex(column)
        val raw = if (index < 0 || isNull(index)) null else getString(index)
        val clean = if (controlAs == null) {
            raw?.filterNot { it.isISOControl() }
        } else {
            raw?.map { if (it.isISOControl()) controlAs else it }?.joinToString("")
        }
        return clean?.trim()?.take(MAX_TEXT)?.takeIf { it.isNotEmpty() }
    }

    /** A tag value. MediaStore writes "<unknown>" where a file has no tag. */
    private fun Cursor.tag(column: String, controlAs: Char? = null): String? =
        text(column, controlAs)?.takeIf { it != MediaStore.UNKNOWN_STRING }

    private fun Cursor.number(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index < 0 || isNull(index)) null else getLong(index)
    }

    internal companion object {
        private const val MS_PER_SECOND = 1000L

        /** The longest tag or path that is kept. Longer text is cut. */
        private const val MAX_TEXT = 512

        /** The longest file name a file system of a phone allows. */
        private const val MAX_FILE_NAME = 255

        private val COLUMNS = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.VOLUME_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.CD_TRACK_NUMBER,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )

        /**
         * Joins the storage name and the folders into one path with no "/"
         * at the ends. Empty parts and the parts "." and ".." are left out.
         */
        fun folderPath(volume: String?, relativePath: String?): String = listOfNotNull(volume, relativePath)
            .flatMap { it.split('/') }
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/")

        /** The number at the start of a tag such as "3/12", or null when there is none. */
        fun leadingNumber(value: String?): Int? =
            value?.trim()?.takeWhile { it.isDigit() }?.takeIf { it.length <= MAX_DIGITS }?.toIntOrNull()?.takeIf {
                it >
                    0
            }

        /** A longer number is no track or disc number; the limit also keeps the value inside an Int. */
        private const val MAX_DIGITS = 6
    }
}
