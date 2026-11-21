/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.siliconcircuits.tarangini.core.data.local.FtsQuery
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.entity.FavouriteSongEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource
import com.siliconcircuits.tarangini.core.data.local.testDatabase
import com.siliconcircuits.tarangini.core.data.settings.SettingsKeys
import com.siliconcircuits.tarangini.core.domain.library.ScanFailure
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.library.ScanSummary
import com.siliconcircuits.tarangini.core.domain.premium.ReportedProblem
import com.siliconcircuits.tarangini.core.testing.premium.FakeCrashReporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultLibraryScannerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = testDatabase()
    private val library = database.libraryDao()
    private val source = FakeAudioFileSource()
    private var now = 1_000_000L

    @After
    fun closeDatabase() = database.close()

    private val crashes = FakeCrashReporter()

    private fun TestScope.settings(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("settings.preferences_pb") }

    private fun TestScope.scanner(settings: DataStore<Preferences> = settings()) =
        DefaultLibraryScanner(source, library, settings, { now }, StandardTestDispatcher(testScheduler), crashes)

    private suspend fun DefaultLibraryScanner.summary(): ScanSummary = (scan() as ScanOutcome.Success).summary

    private suspend fun titles() = library.songsByTitle(limit = 100, offset = 0).map { it.song.title }

    @Test
    fun `a first scan saves the songs with their artist, album and folders`() = runTest {
        source.files = listOf(
            audioFile("1", title = "Bhairavi", artist = "Ravi", album = "Morning", folderPath = "$MUSIC/Ragas"),
            audioFile("2", title = "Yaman", artist = "Ravi", album = "Evening", folderPath = MUSIC),
        )

        val summary = scanner().summary()

        assertEquals(ScanSummary(added = 2, updated = 0, removed = 0, unchanged = 0), summary)
        assertEquals(listOf("Bhairavi", "Yaman"), titles())
        assertEquals(listOf("Ravi"), library.artists(limit = 10, offset = 0).map { it.artist.name })
        assertEquals(listOf("Evening", "Morning"), library.albums(limit = 10, offset = 0).map { it.album.title })
        val top = library.childFolders(null).single()
        assertEquals("external_primary", top.path)
        val music = library.childFolders(top.id).single()
        assertEquals("Music", music.name)
        assertEquals(listOf("Yaman"), library.songsInFolder(music.id, limit = 10, offset = 0).map { it.song.title })
        assertEquals(listOf("Ragas"), library.childFolders(music.id).map { it.name })
    }

    @Test
    fun `a second scan with no change writes nothing`() = runTest {
        source.files = listOf(audioFile("1"), audioFile("2"))
        val scanner = scanner()
        scanner.scan()
        val before = library.songKeys(SongSource.LOCAL)

        val summary = scanner.summary()

        assertEquals(ScanSummary(added = 0, updated = 0, removed = 0, unchanged = 2), summary)
        assertEquals(before, library.songKeys(SongSource.LOCAL))
    }

    @Test
    fun `a file with no tags shows its file name and joins the artist and album with no name`() = runTest {
        source.files = listOf(
            audioFile("1", title = null, fileName = "My Recording.v2.mp3", artist = null, album = null),
        )

        scanner().scan()

        val song = library.songsByTitle(limit = 10, offset = 0).single()
        assertEquals("My Recording.v2", song.song.title)
        assertEquals("", song.artistName)
        assertEquals("", song.albumTitle)
    }

    @Test
    fun `a changed file is saved again and keeps its row`() = runTest {
        source.files = listOf(audioFile("1", title = "Old title"))
        val scanner = scanner()
        scanner.scan()
        val id = library.songKeys(SongSource.LOCAL).single().id
        source.files = listOf(audioFile("1", title = "New title", dateModified = 2_000))

        val summary = scanner.summary()

        assertEquals(ScanSummary(added = 0, updated = 1, removed = 0, unchanged = 0), summary)
        assertEquals("New title", library.song(id)?.song?.title)
        assertEquals(listOf("New title"), library.searchSongsByPrefix("new"))
    }

    @Test
    fun `a song whose file is gone leaves the lists and keeps its favourite until the file is back`() = runTest {
        source.files = listOf(audioFile("1", title = "Stays"), audioFile("2", title = "Goes"))
        val scanner = scanner()
        scanner.scan()
        val gone = library.songKeys(SongSource.LOCAL).first { it.sourceId == "2" }.id
        database.favouriteDao().addKnownSong(gone, addedAt = 1)

        source.files = listOf(audioFile("1", title = "Stays"))
        val summary = scanner.summary()

        assertEquals(ScanSummary(added = 0, updated = 0, removed = 1, unchanged = 1), summary)
        assertEquals(listOf("Stays"), titles())
        assertEquals(now, library.song(gone)?.song?.missingSince)
        assertTrue(gone in database.favouriteDao().favouriteSongIds().first())

        source.files = listOf(audioFile("1", title = "Stays"), audioFile("2", title = "Goes"))
        scanner.scan()

        assertEquals(listOf("Goes", "Stays"), titles())
        assertEquals(gone, library.songKeys(SongSource.LOCAL).first { it.sourceId == "2" }.id)
    }

    @Test
    fun `a file that comes back with a new id is the same song and keeps its favourite`() = runTest {
        val file = audioFile("1", title = "Raga", fileName = "raga.mp3", sizeBytes = 4_000)
        source.files = listOf(file)
        val scanner = scanner()
        scanner.scan()
        val id = library.songKeys(SongSource.LOCAL).single().id
        database.favouriteDao().addKnownSong(id, addedAt = 1)
        source.files = emptyList()
        scanner.scan()

        source.files = listOf(file.copy(id = "77"))
        scanner.scan()

        val row = library.songKeys(SongSource.LOCAL).single()
        assertEquals(id, row.id)
        assertEquals("77", row.sourceId)
        assertNull(row.missingSince)
        assertTrue(id in database.favouriteDao().favouriteSongIds().first())
    }

    @Test
    fun `a song that has been gone for 30 days is removed for good, with its artist, album and folder`() = runTest {
        source.files = listOf(audioFile("1"))
        val scanner = scanner()
        scanner.scan()
        source.files = emptyList()
        scanner.scan()

        now += THIRTY_DAYS_MS - 1
        scanner.scan()
        assertEquals(1, library.songKeys(SongSource.LOCAL).size)

        now += 2
        scanner.scan()

        assertTrue(library.songKeys(SongSource.LOCAL).isEmpty())
        assertTrue(library.childFolders(null).isEmpty())
        assertEquals(0, library.deleteUnusedAlbums() + library.deleteUnusedArtists())
    }

    @Test
    fun `the settings leave out short files and excluded folders, also when they were in the library`() = runTest {
        source.files = listOf(
            audioFile("1", title = "Song"),
            audioFile("2", title = "Voice note", durationMs = 4_000),
            audioFile("3", title = "Call", folderPath = "external_primary/Recordings/Calls"),
        )
        val settings = settings()
        val scanner = scanner(settings)
        scanner.scan()
        assertEquals(3, titles().size)

        settings.edit {
            it[SettingsKeys.MIN_TRACK_SECONDS] = 30
            it[SettingsKeys.EXCLUDED_FOLDERS] = setOf("external_primary/Recordings")
        }
        val summary = scanner.summary()

        assertEquals(2, summary.removed)
        assertEquals(listOf("Song"), titles())
    }

    @Test
    fun `an album by many artists is one album under the album artist`() = runTest {
        source.files = listOf(
            audioFile("1", artist = "Asha", albumArtist = "Various", album = "Hits"),
            audioFile("2", artist = "Kishore", albumArtist = "Various", album = "Hits"),
        )

        scanner().scan()

        val album = library.albums(limit = 10, offset = 0).single()
        assertEquals("Various", album.artistName)
        assertEquals(2, album.songCount)
    }

    @Test
    fun `with no permission the scan fails and the library stays as it is`() = runTest {
        source.files = listOf(audioFile("1"))
        val scanner = scanner()
        scanner.scan()
        source.permission = false

        val outcome = scanner.scan()

        assertEquals(ScanOutcome.Failure(ScanFailure.NO_PERMISSION), outcome)
        assertEquals(ScanState.Finished(outcome), scanner.state.value)
        assertEquals(1, titles().size)
    }

    @Test
    fun `when the phone gives no file list the scan fails and marks nothing as gone`() = runTest {
        source.files = listOf(audioFile("1"))
        val scanner = scanner()
        scanner.scan()
        source.files = null

        scanner.scan()
        val outcome = scanner.scan()

        assertEquals(ScanOutcome.Failure(ScanFailure.STORAGE_UNAVAILABLE), outcome)
        assertEquals(listOf(ReportedProblem.SCAN_FAILED to "STORAGE_UNAVAILABLE"), crashes.reports)
        assertEquals(1, titles().size)
    }

    @Test
    fun `a library of 10,000 songs is saved and a second scan finds no change`() = runTest {
        source.files = (1..LARGE_LIBRARY).map {
            audioFile(
                "$it",
                artist = "Artist ${it % 300}",
                album = "Album ${it % 900}",
                folderPath = "$MUSIC/${it % 50}",
            )
        }
        val scanner = scanner()

        assertEquals(LARGE_LIBRARY, scanner.summary().added)
        assertEquals(LARGE_LIBRARY, scanner.summary().unchanged)
        assertNotNull(library.songsByTitle(limit = 1, offset = LARGE_LIBRARY - 1).singleOrNull())
    }

    @Test
    fun `a song that the settings hide is kept past 30 days and comes back with its favourite`() = runTest {
        source.files = listOf(audioFile("1", title = "Short", durationMs = 4_000))
        val settings = settings()
        val scanner = scanner(settings)
        scanner.scan()
        val id = library.songKeys(SongSource.LOCAL).single().id
        database.favouriteDao().addKnownSong(id, addedAt = 1)

        settings.edit { it[SettingsKeys.MIN_TRACK_SECONDS] = 30 }
        scanner.scan()
        now += THIRTY_DAYS_MS * 2
        scanner.scan()
        assertTrue(titles().isEmpty())
        assertEquals(id, library.songKeys(SongSource.LOCAL).single().id)

        settings.edit { it[SettingsKeys.MIN_TRACK_SECONDS] = 0 }
        scanner.scan()

        assertEquals(listOf("Short"), titles())
        assertTrue(id in database.favouriteDao().favouriteSongIds().first())
    }

    @Test
    fun `a hidden song whose file goes away later is still kept for 30 days from then`() = runTest {
        val file = audioFile("1", title = "Short", durationMs = 4_000)
        source.files = listOf(file)
        val settings = settings()
        val scanner = scanner(settings)
        scanner.scan()
        settings.edit { it[SettingsKeys.MIN_TRACK_SECONDS] = 30 }
        scanner.scan()

        now += THIRTY_DAYS_MS * 2
        source.files = emptyList()
        scanner.scan()
        assertEquals(now, library.songKeys(SongSource.LOCAL).single().missingSince)

        now += THIRTY_DAYS_MS - 1
        source.files = listOf(file)
        settings.edit { it[SettingsKeys.MIN_TRACK_SECONDS] = 0 }
        scanner.scan()

        assertEquals(listOf("Short"), titles())
    }

    @Test
    fun `an album gets its year from the first song that has one`() = runTest {
        source.files = listOf(audioFile("1", album = "Hits"), audioFile("2", album = "Hits").copy(year = 1998))

        scanner().scan()

        assertEquals(1998, library.albums(limit = 10, offset = 0).single().album.year)
    }

    @Test
    fun `a file named only by its ending shows its whole name`() = runTest {
        source.files = listOf(audioFile("1", title = null, fileName = ".mp3"))

        scanner().scan()

        assertEquals(listOf(".mp3"), titles())
    }

    @Test
    fun `a scan that ends with an error does not stay in the running state, and the next scan works`() = runTest {
        val scanner = scanner()
        source.files = listOf(audioFile("1"))
        source.failure = IllegalStateException("the phone stopped the read")

        val error = runCatching { scanner.scan() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(ScanState.Idle, scanner.state.value)

        source.failure = null
        assertEquals(1, scanner.summary().added)
    }

    @Test
    fun `two scans never run at the same time`() = runTest {
        var running = 0
        var mostRunning = 0
        val probe = object : AudioFileSource by source {
            override fun hasPermission(): Boolean {
                running++
                mostRunning = maxOf(mostRunning, running)
                return true
            }

            override fun files(): List<AudioFile> {
                running--
                return emptyList()
            }
        }
        val scanner =
            DefaultLibraryScanner(probe, library, settings(), { now }, StandardTestDispatcher(testScheduler), crashes)

        val scans = List(2) { launch { scanner.scan() } }
        scans.forEach { it.join() }

        assertEquals(1, mostRunning)
    }

    private suspend fun LibraryDao.searchSongsByPrefix(word: String): List<String> =
        checkNotNull(FtsQuery.prefix(word)).let { searchSongs(it, it, "", limit = 10) }.map { it.song.title }

    private companion object {
        const val MUSIC = "external_primary/Music"
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
        const val LARGE_LIBRARY = 10_000
    }
}
