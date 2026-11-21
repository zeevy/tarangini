/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.backup

import android.net.Uri
import com.siliconcircuits.tarangini.core.data.local.addSong
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.data.repository.DefaultFavouriteRepository
import com.siliconcircuits.tarangini.core.data.repository.DefaultPlaylistRepository
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.BackupRestore
import com.siliconcircuits.tarangini.core.testing.settings.FakeSettingsRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
class DefaultBackupFilesTest {
    private val context = RuntimeEnvironment.getApplication()
    private val resolver = shadowOf(context.contentResolver)
    private val database = testDatabase()
    private val playlists = DefaultPlaylistRepository(database.playlistDao(), database.playlistFileDao()) { 1L }
    private var clock = 0L
    private val favourites = DefaultFavouriteRepository(database.favouriteDao()) { ++clock }
    private val settings = FakeSettingsRepository().apply {
        folders = listOf("external_primary/Calls", "external_primary/Music", "external_primary/Podcasts")
    }
    private val sources = BackupSources(database.backupDao(), playlists, settings)
    private val files = DefaultBackupFiles(context, sources, { 1_000L }, Dispatchers.Unconfined)
    private val picked = Uri.parse("content://documents/tree/1")

    @After
    fun closeDatabase() = database.close()

    /** Three songs in `external_primary/Music`. Returns their ids. */
    private suspend fun library(): List<Long> {
        val storage = database.libraryDao().saveFolder("external_primary", "external_primary", parentId = null)
        val music = database.libraryDao().saveFolder("external_primary/Music", "Music", parentId = storage)
        return listOf("One", "Two", "Three").mapIndexed { index, title ->
            database.addSong(title, sourceId = "${index + 1}", folderId = music)
        }
    }

    /** Favourites Two then One, a playlist "Drive" with Three and One, and settings that are not the defaults. */
    private suspend fun thingsOfThePerson(ids: List<Long>) {
        favourites.setFavourite(ids[1], true)
        favourites.setFavourite(ids[0], true)
        playlists.addAll(requireNotNull(playlists.create("Drive")), listOf(ids[2], ids[0]))
        settings.scan.value = ScanSettings(30, setOf("external_primary/Calls"))
        settings.setSongSort(SongSort.ARTIST)
    }

    private suspend fun forgetEverything() {
        favourites.songIds().first().forEach { favourites.setFavourite(it, false) }
        playlists.playlists().forEach { playlists.delete(it.id) }
        settings.scan.value = ScanSettings()
        settings.setSongSort(SongSort.TITLE)
    }

    private suspend fun backup(): String {
        val written = ByteArrayOutputStream()
        resolver.registerOutputStream(picked, written)
        assertTrue(files.backUpTo(picked.toString()))
        return written.toString(Charsets.UTF_8)
    }

    private suspend fun restore(text: String, replace: Boolean = false): BackupRestore {
        resolver.registerInputStream(picked, ByteArrayInputStream(text.toByteArray()))
        return files.restoreFrom(picked.toString(), replace)
    }

    private suspend fun favouriteTitles() = favourites.songs(Page(100)).map { it.title }

    private suspend fun playlistTitles(): Map<String, List<String>> = playlists.playlists().associate {
        it.name to playlists.songs(it.id, Page(100)).map { place -> place.song.title }
    }

    @Test
    fun `a backup brings back the favourites in their order, the playlists and the settings`() = runTest {
        thingsOfThePerson(library())
        val text = backup()
        forgetEverything()

        val done = restore(text)

        assertEquals(BackupRestore.Done(favourites = 2, playlists = 1, songsNotFound = 0), done)
        assertEquals(listOf("One", "Two"), favouriteTitles())
        assertEquals(mapOf("Drive" to listOf("Three", "One")), playlistTitles())
        assertEquals(ScanSettings(30, setOf("external_primary/Calls")), settings.scan.value)
        assertEquals(SongSort.ARTIST, settings.songSort().first())
    }

    @Test
    fun `without replace what is here stays, and a playlist with a name that is here is not made twice`() = runTest {
        val ids = library()
        thingsOfThePerson(ids)
        val text = backup()
        favourites.setFavourite(ids[2], true)
        playlists.create("Night")
        settings.setFolderHidden("external_primary/Podcasts", true)

        val done = restore(text)

        assertEquals(0, (done as BackupRestore.Done).playlists)
        assertEquals(listOf("Three", "One", "Two"), favouriteTitles())
        assertEquals(setOf("Drive", "Night"), playlistTitles().keys)
        assertEquals(setOf("external_primary/Calls", "external_primary/Podcasts"), settings.scan.value.hiddenFolders)
    }

    @Test
    fun `with replace only what the file holds is left`() = runTest {
        val ids = library()
        thingsOfThePerson(ids)
        val text = backup()
        favourites.setFavourite(ids[2], true)
        playlists.create("Night")
        settings.setFolderHidden("external_primary/Podcasts", true)

        restore(text, replace = true)

        assertEquals(listOf("One", "Two"), favouriteTitles())
        assertEquals(mapOf("Drive" to listOf("Three", "One")), playlistTitles())
        assertEquals(setOf("external_primary/Calls"), settings.scan.value.hiddenFolders)
    }

    @Test
    fun `a file that is no backup, too new or too large changes nothing`() = runTest {
        thingsOfThePerson(library())

        assertEquals(BackupRestore.NotABackup, restore("#EXTM3U\nMusic/One.mp3", replace = true))
        assertEquals(BackupRestore.NotABackup, restore("""{"format": "other", "version": 1}""", replace = true))
        assertEquals(BackupRestore.NotABackup, restore("[1, 2]", replace = true))
        assertEquals(
            BackupRestore.NewerVersion,
            restore("""{"format": "tarangini-backup", "version": 2}""", replace = true),
        )
        assertEquals(BackupRestore.TooLarge, restore("x".repeat(BackupFormat.MAX_BYTES + 1), replace = true))
        assertEquals(listOf("One", "Two"), favouriteTitles())
        assertEquals(mapOf("Drive" to listOf("Three", "One")), playlistTitles())
    }

    @Test
    fun `values that are not what they should be are left out, and songs that are not here are counted`() = runTest {
        library()
        val text = """
            {"format": "tarangini-backup", "version": 1,
             "settings": {"minTrackSeconds": 99999, "songSort": "LOUDEST", "hiddenFolders": [7, "external_primary", "Somewhere/Else", ""]},
             "favourites": ["external_primary/Music/Two.mp3", 12, {"x": 1}, "D:\\Music\\One.mp3", "Music/gone.mp3"],
             "playlists": [{"name": "  ", "songs": ["Music/One.mp3"]}, "no playlist",
                           {"name": "Mix", "songs": "no list"},
                           {"name": "Ca\u202Er", "songs": ["Music/Three.mp3", "Music/Three.mp3"]}]}
        """.trimIndent()

        val done = restore(text)

        assertEquals(BackupRestore.Done(favourites = 2, playlists = 2, songsNotFound = 1), done)
        assertEquals(listOf("One", "Two"), favouriteTitles())
        // A song can be in a playlist twice. The mark that turns the text around is taken out of the name.
        assertEquals(mapOf("Mix" to emptyList(), "Car" to listOf("Three", "Three")), playlistTitles())
        // A folder the library does not know is not hidden: not a whole storage, not a made up path.
        assertEquals(ScanSettings(0, emptySet()), settings.scan.value)
        assertEquals(SongSort.TITLE, settings.songSort().first())
    }

    @Test
    fun `a song whose file is gone for now is in the backup and keeps its place with replace`() = runTest {
        val ids = library()
        thingsOfThePerson(ids)
        database.libraryDao().markMissing(listOf(ids[0]), 9L)
        val text = backup()

        val done = restore(text, replace = true)

        assertEquals(BackupRestore.Done(favourites = 2, playlists = 1, songsNotFound = 0), done)
        assertEquals(setOf(ids[0], ids[1]), favourites.songIds().first())
        assertEquals(
            listOf(ids[2], ids[0]),
            database.backupDao().playlistFiles(playlists.playlists().single().id).map {
                it.songId
            },
        )
    }

    @Test
    fun `a file name with a joiner of an Indian script is found again`() = runTest {
        val storage = database.libraryDao().saveFolder("external_primary", "external_primary", parentId = null)
        val id = database.addSong("അവന\u0D4D\u200D", sourceId = "1", folderId = storage)
        favourites.setFavourite(id, true)
        val text = backup()
        favourites.setFavourite(id, false)

        val done = restore(text)

        assertEquals(BackupRestore.Done(favourites = 1, playlists = 0, songsNotFound = 0), done)
    }

    @Test
    fun `an empty backup with replace leaves no favourite and no playlist`() = runTest {
        thingsOfThePerson(library())

        restore("""{"format": "tarangini-backup", "version": 1}""", replace = true)

        assertEquals(emptyList<String>(), favouriteTitles())
        assertEquals(emptyMap<String, List<String>>(), playlistTitles())
    }

    @Test
    fun `before the first scan a backup with songs is not read, so no favourite is lost`() = runTest {
        val text = """{"format": "tarangini-backup", "version": 1, "favourites": ["Music/One.mp3"]}"""

        assertEquals(BackupRestore.NoLibrary, restore(text))
    }

    @Test
    fun `only an address of the file picker is opened`() = runTest {
        assertFalse(files.backUpTo("file:///data/data/app/databases/tarangini.db"))
        assertEquals(BackupRestore.Failed, files.restoreFrom("file:///data/data/app/shared_prefs/x.xml", false))
    }
}
