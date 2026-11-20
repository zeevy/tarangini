/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class MediaStoreAudioSourceTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val source = MediaStoreAudioSource(application)

    /** Stands in for the media index of the phone and answers every query with [rows]. */
    private class FakeMediaProvider(private val rows: List<Map<String, Any?>>?) : ContentProvider() {
        var askedUri: Uri? = null

        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? {
            askedUri = uri
            val columns = checkNotNull(projection)
            return rows?.let { all ->
                MatrixCursor(columns).apply { all.forEach { row -> addRow(columns.map { row[it] }) } }
            }
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }

    private fun mediaIndexWith(rows: List<Map<String, Any?>>?): FakeMediaProvider =
        FakeMediaProvider(rows).also { ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, it) }

    @Test
    fun `a row of the media index becomes an audio file`() {
        val provider = mediaIndexWith(
            listOf(
                mapOf(
                    MediaStore.Audio.Media._ID to 42L,
                    MediaStore.Audio.Media.DISPLAY_NAME to "yaman.mp3",
                    MediaStore.Audio.Media.VOLUME_NAME to "external_primary",
                    MediaStore.Audio.Media.RELATIVE_PATH to "Music/Ragas/",
                    MediaStore.Audio.Media.MIME_TYPE to "audio/mpeg",
                    MediaStore.Audio.Media.TITLE to " Yaman ",
                    MediaStore.Audio.Media.ARTIST to "Ravi",
                    MediaStore.Audio.Media.ALBUM_ARTIST to "Various",
                    MediaStore.Audio.Media.ALBUM to "Evening",
                    MediaStore.Audio.Media.DURATION to 185_000L,
                    MediaStore.Audio.Media.SIZE to 4_400_000L,
                    MediaStore.Audio.Media.CD_TRACK_NUMBER to "3/12",
                    MediaStore.Audio.Media.DISC_NUMBER to "1",
                    MediaStore.Audio.Media.YEAR to 1998L,
                    MediaStore.Audio.Media.DATE_ADDED to 1_700_000_000L,
                    MediaStore.Audio.Media.DATE_MODIFIED to 1_700_000_500L,
                ),
            ),
        )

        val file = source.files()?.single()

        assertEquals(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), provider.askedUri)
        assertEquals(
            AudioFile(
                id = "42",
                fileName = "yaman.mp3",
                folderPath = "external_primary/Music/Ragas",
                mimeType = "audio/mpeg",
                title = "Yaman",
                artist = "Ravi",
                albumArtist = "Various",
                album = "Evening",
                durationMs = 185_000,
                sizeBytes = 4_400_000,
                trackNumber = 3,
                discNumber = 1,
                year = 1998,
                dateAdded = 1_700_000_000_000,
                dateModified = 1_700_000_500_000,
            ),
            file,
        )
    }

    @Test
    fun `a file with no tags has no title, artist, album, length or numbers`() {
        mediaIndexWith(
            listOf(
                mapOf(
                    MediaStore.Audio.Media._ID to 7L,
                    MediaStore.Audio.Media.DISPLAY_NAME to "rec.m4a",
                    MediaStore.Audio.Media.ARTIST to MediaStore.UNKNOWN_STRING,
                    MediaStore.Audio.Media.ALBUM to "  ",
                    MediaStore.Audio.Media.DURATION to 0L,
                    MediaStore.Audio.Media.YEAR to 0L,
                    MediaStore.Audio.Media.CD_TRACK_NUMBER to "A1",
                ),
            ),
        )

        val file = checkNotNull(source.files()).single()

        assertNull(file.title)
        assertNull(file.artist)
        assertNull(file.albumArtist)
        assertNull(file.album)
        assertNull(file.durationMs)
        assertNull(file.trackNumber)
        assertNull(file.discNumber)
        assertNull(file.year)
        assertEquals("", file.folderPath)
        assertEquals(0L, file.sizeBytes)
    }

    @Test
    fun `text from a file is cut to a safe length and loses its control characters`() {
        mediaIndexWith(
            listOf(
                mapOf(
                    MediaStore.Audio.Media._ID to 9L,
                    MediaStore.Audio.Media.DISPLAY_NAME to "a".repeat(1_000) + ".mp3",
                    MediaStore.Audio.Media.TITLE to "Ya\u0000man\n" + "x".repeat(5_000),
                    MediaStore.Audio.Media.ARTIST to "\u0007\t",
                ),
            ),
        )

        val file = checkNotNull(source.files()).single()

        assertEquals(255, file.fileName.length)
        assertEquals(512, file.title?.length)
        assertTrue(checkNotNull(file.title).startsWith("Yamanx"))
        assertNull(file.artist)
    }

    @Test
    fun `no answer from the media index gives no list`() {
        mediaIndexWith(rows = null)

        assertNull(source.files())
    }

    @Test
    fun `the permission check follows the audio permission`() {
        assertFalse(source.hasPermission())

        shadowOf(application).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)

        assertTrue(source.hasPermission())
    }

    @Test
    fun `the folder path has no slash at the ends and no empty part`() {
        assertEquals("external_primary/Music/Rock", MediaStoreAudioSource.folderPath("external_primary", "Music/Rock/"))
        assertEquals("external_primary", MediaStoreAudioSource.folderPath("external_primary", "/"))
        assertEquals("Music", MediaStoreAudioSource.folderPath(null, "/Music//"))
        assertEquals("", MediaStoreAudioSource.folderPath(null, null))
        assertEquals("external_primary/Music", MediaStoreAudioSource.folderPath("external_primary", "../Music/./"))
    }

    @Test
    fun `a track or disc tag gives its first number`() {
        assertEquals(3, MediaStoreAudioSource.leadingNumber("3/12"))
        assertEquals(12, MediaStoreAudioSource.leadingNumber(" 12 "))
        assertNull(MediaStoreAudioSource.leadingNumber("0"))
        assertNull(MediaStoreAudioSource.leadingNumber("A1"))
        assertNull(MediaStoreAudioSource.leadingNumber(null))
        assertNull(MediaStoreAudioSource.leadingNumber("12345678901234567890"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `a change in the media index of the phone is passed on`() = runTest {
        val seen = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.changes().toList(seen) }

        val song = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 7)
        application.contentResolver.notifyChange(song, null)
        runCurrent()

        assertEquals(1, seen.size)
    }
}
