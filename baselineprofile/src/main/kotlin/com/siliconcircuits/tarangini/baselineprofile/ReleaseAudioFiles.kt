/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.baselineprofile

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.junit.rules.ExternalResource

/**
 * Adds three test songs to the music of the device before a test and removes them after it. This
 * test app adds them as its own files, so it needs no permission, and the app under test finds
 * them in its scan like any other song. The files are the ones of the in-app end to end test.
 */
class ReleaseAudioFiles(private val context: Context) : ExternalResource() {
    private val added = mutableListOf<Uri>()

    /** The titles the media index gave the songs, in their order. */
    var titles: List<String> = emptyList()
        private set

    override fun before() {
        // A number of this run, so a second run on the same device makes new songs.
        val run = System.currentTimeMillis() % RUN_RANGE
        val resolver = context.contentResolver
        titles = (1..SONGS).map { number ->
            val pending = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "Tarangini release $run $number.mp3")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, FOLDER)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = requireNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, pending))
            added += uri
            requireNotNull(resolver.openOutputStream(uri)).use { out ->
                context.assets.open("e2e/song-$number.mp3").use { it.copyTo(out) }
            }
            // The media index reads the file when it is no longer pending.
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            titleOf(uri)
        }
    }

    override fun after() {
        added.forEach { context.contentResolver.delete(it, null, null) }
        added.clear()
    }

    private fun titleOf(uri: Uri): String {
        val query = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.TITLE),
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(ContentUris.parseId(uri).toString()),
            null,
        )
        return requireNotNull(query).use { cursor ->
            check(cursor.moveToFirst()) { "The media index has no song $uri" }
            cursor.getString(0)
        }
    }

    private companion object {
        const val SONGS = 3
        const val RUN_RANGE = 100_000L
        const val FOLDER = "Music/Tarangini release test"
    }
}
