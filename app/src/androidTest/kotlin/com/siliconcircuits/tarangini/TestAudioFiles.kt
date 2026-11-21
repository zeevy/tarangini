/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.provider.MediaStore
import org.junit.rules.ExternalResource

/**
 * Adds the three test songs of the assets to the music of the device before a test, and removes
 * them after it. The app adds them as its own files, so it needs no write permission, and they
 * get the names `Tarangini test <run> <n>`, which the media index takes as their titles.
 */
class TestAudioFiles(private val context: Context, private val assets: AssetManager) : ExternalResource() {
    /** A number of this run, so a second run on the same device makes new songs. */
    val run: Long = System.currentTimeMillis() % RUN_RANGE

    private val added = mutableListOf<Uri>()

    /** The titles the media index gave the songs, in their order. */
    var titles: List<String> = emptyList()
        private set

    override fun before() {
        val resolver = context.contentResolver
        titles = (1..SONGS).map { number ->
            val pending = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "Tarangini test $run $number.mp3")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, FOLDER)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = requireNotNull(resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, pending))
            added += uri
            requireNotNull(resolver.openOutputStream(uri)).use { out ->
                assets.open("e2e/song-$number.mp3").use { it.copyTo(out) }
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
        val id = ContentUris.parseId(uri)
        val columns = arrayOf(MediaStore.Audio.Media.TITLE)
        val query = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns,
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(id.toString()),
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
        const val FOLDER = "Music/Tarangini test"
    }
}
