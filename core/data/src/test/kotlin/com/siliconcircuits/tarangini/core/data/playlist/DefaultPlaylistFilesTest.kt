/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.playlist

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.data.repository.DefaultPlaylistRepository
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.playlist.M3u
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistImport
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class DefaultPlaylistFilesTest {
    private val context = RuntimeEnvironment.getApplication()
    private val resolver = shadowOf(context.contentResolver)
    private val database = testDatabase()
    private val playlists = DefaultPlaylistRepository(database.playlistDao(), database.playlistFileDao()) { 1L }
    private val files = DefaultPlaylistFiles(context, database.playlistFileDao(), playlists, Dispatchers.Unconfined)
    private val picked = Uri.parse("content://documents/tree/1")

    @After
    fun closeDatabase() = database.close()

    /** Three songs in `external_primary/Music/Ragas`, in a playlist in another order than their ids. */
    private suspend fun drive(): Long {
        val library = database.libraryDao()
        val storage = library.saveFolder("external_primary", "external_primary", parentId = null)
        val music = library.saveFolder("external_primary/Music", "Music", parentId = storage)
        val ragas = library.saveFolder("external_primary/Music/Ragas", "Ragas", parentId = music)
        // A local song has a number as its file id.
        val ids = listOf("One", "Two", "Three").mapIndexed { index, title ->
            database.addSong(title, sourceId = "${index + 1}", folderId = ragas)
        }
        val id = requireNotNull(playlists.create("Drive"))
        playlists.addAll(id, listOf(ids[2], ids[0], ids[1], ids[2]))
        return id
    }

    private fun pickFile(text: ByteArray, name: String? = "Evening drive.m3u") {
        resolver.registerInputStream(picked, ByteArrayInputStream(text))
        ShadowContentResolver.registerProviderInternal(picked.authority, NamedFiles(name))
    }

    private suspend fun exported(playlistId: Long): String {
        val written = ByteArrayOutputStream()
        resolver.registerOutputStream(picked, written)
        assertTrue(files.exportTo(playlistId, picked.toString()))
        return written.toString(Charsets.UTF_8)
    }

    private suspend fun titles(playlistId: Long) = playlists.songs(playlistId, Page(100)).map { it.song.title }

    @Test
    fun `an exported file names each song by its path on the phone and imports back to the same playlist`() = runTest {
        val text = exported(drive())
        pickFile(text.toByteArray())

        val done = files.importFrom(picked.toString()) as PlaylistImport.Done

        assertTrue(M3u.entries(text).all { it.startsWith("/storage/emulated/0/Music/Ragas/") })
        assertEquals(listOf("Three", "One", "Two", "Three"), titles(done.playlistId))
        assertEquals("Evening drive", done.name)
        assertEquals(Triple(4, 4, emptyList<String>()), Triple(done.matched, done.total, done.notFound))
    }

    @Test
    fun `Windows paths and relative paths match, and what is not in the library is listed`() = runTest {
        val names = M3u.entries(exported(drive())).map { it.substringAfterLast('/') }
        val lines = listOf(
            "#EXTM3U",
            "D:\\Music\\Ragas\\${names[0]}",
            "Ragas/${names[1]}",
            "https://example.com/stream",
            "Ragas/not here.mp3",
        )
        pickFile(lines.joinToString("\r\n").toByteArray(), name = null)

        val done = files.importFrom(picked.toString()) as PlaylistImport.Done

        assertEquals(listOf("Three", "One"), titles(done.playlistId))
        assertEquals("Imported playlist", done.name)
        assertEquals(2 to 4, done.matched to done.total)
        assertEquals(listOf("https://example.com/stream", "Ragas/not here.mp3"), done.notFound)
    }

    @Test
    fun `a file that is too large is refused and makes no playlist`() = runTest {
        pickFile(ByteArray(50 * 1024 * 1024) { 'x'.code.toByte() })

        assertEquals(PlaylistImport.TooLarge, files.importFrom(picked.toString()))
        assertEquals(emptyList<String>(), playlists.playlists().map { it.name })
    }

    @Test
    fun `a file with no song of the library makes no playlist`() = runTest {
        drive()
        pickFile(byteArrayOf(0, 1, 2, 10, 13, -1, 40, 41))

        assertEquals(PlaylistImport.NothingFound, files.importFrom(picked.toString()))
        assertEquals(listOf("Drive"), playlists.playlists().map { it.name })
    }

    @Test
    fun `only an address of the file picker is opened`() = runTest {
        val id = drive()

        assertEquals(PlaylistImport.Failed, files.importFrom("file:///data/data/app/databases/tarangini.db"))
        assertEquals(PlaylistImport.Failed, files.importFrom("content://documents/nothing/here"))
        assertFalse(files.exportTo(id, "file:///sdcard/out.m3u"))
    }
}

/** A file provider that only knows the name of its file, as the file picker tells it. */
private class NamedFiles(private val name: String?) : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        columns: Array<String>?,
        where: String?,
        values: Array<String>?,
        sort: String?,
    ): Cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { name?.let { addRow(arrayOf(it)) } }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, where: String?, values: Array<String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, where: String?, whereValues: Array<String>?): Int = 0
}
