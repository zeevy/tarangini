/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.siliconcircuits.tarangini.core.data.di.IoDispatcher
import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.entity.SongEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource
import com.siliconcircuits.tarangini.core.data.local.sortKey
import com.siliconcircuits.tarangini.core.data.settings.SettingsKeys
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.library.ScanFailure
import com.siliconcircuits.tarangini.core.domain.library.ScanOutcome
import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.library.ScanSummary
import com.siliconcircuits.tarangini.core.domain.premium.CrashReporter
import com.siliconcircuits.tarangini.core.domain.premium.ReportedProblem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Brings the library in step with the audio files of the phone.
 *
 * A scan reads the whole file list once, compares it with the stored songs
 * and writes only the difference. A scan with nothing new writes nothing.
 *
 * A song whose file is gone is not removed at once. It is marked and leaves
 * the lists, and its favourite and playlist places stay for [KEEP_MISSING_MS],
 * so a memory card that is put back loses nothing. After that time the song
 * is removed for good.
 */
@Singleton
internal class DefaultLibraryScanner @Inject constructor(
    private val source: AudioFileSource,
    private val library: LibraryDao,
    private val settings: DataStore<Preferences>,
    private val time: TimeSource,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val crashes: CrashReporter,
) : LibraryScanner {
    private val scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val state: StateFlow<ScanState> = scanState.asStateFlow()

    /** Two scans at once would both add the same new file. */
    private val oneScan = Mutex()

    // Only read and written inside a scan, so the lock of the scan guards it.
    private var failureReported = false

    override suspend fun scan(): ScanOutcome = oneScan.withLock {
        scanState.value = ScanState.Running(done = 0, total = 0)
        var outcome: ScanOutcome? = null
        try {
            outcome = runScan()
            outcome
        } finally {
            // A scan that was cancelled or failed with an error must not leave "running" behind.
            scanState.value = outcome?.let { ScanState.Finished(it) } ?: ScanState.Idle
        }
    }

    private suspend fun runScan(): ScanOutcome {
        if (!source.hasPermission()) return ScanOutcome.Failure(ScanFailure.NO_PERMISSION)
        val files = withContext(io) { source.files() }
        // No answer is never read as "all files are gone".
        return if (files == null) {
            // A missing permission is the choice of the person. This one is a fault of the phone.
            // It is reported once, not at every rescan while the storage is away.
            if (!failureReported) crashes.report(ReportedProblem.SCAN_FAILED, ScanFailure.STORAGE_UNAVAILABLE.name)
            failureReported = true
            ScanOutcome.Failure(ScanFailure.STORAGE_UNAVAILABLE)
        } else {
            failureReported = false
            ScanOutcome.Success(bringInStep(files, filterFromSettings()))
        }
    }

    /** Writes the difference between the stored songs and [files], and says what changed. */
    private suspend fun bringInStep(files: List<AudioFile>, filter: ScanFilter): ScanSummary {
        val now = time.nowMs()
        val plan = planScan(
            stored = library.songKeys(SongSource.LOCAL),
            found = files.filter(filter::accepts),
            hiddenFiles = files.filter(filter::hides),
            expiredBefore = now - KEEP_MISSING_MS,
        )
        if (!plan.isEmpty) write(plan, now)
        return ScanSummary(
            added = plan.added.size,
            updated = plan.updated.size,
            removed = plan.gone.size + plan.hidden.size,
            unchanged = plan.unchanged,
        )
    }

    private suspend fun filterFromSettings(): ScanFilter {
        val saved = settings.data.first()
        return ScanFilter(
            minTrackMs = (saved[SettingsKeys.MIN_TRACK_SECONDS] ?: 0) * MS_PER_SECOND,
            includedFolders = saved[SettingsKeys.INCLUDED_FOLDERS].orEmpty(),
            excludedFolders = saved[SettingsKeys.EXCLUDED_FOLDERS].orEmpty(),
        )
    }

    private suspend fun write(plan: ScanPlan, now: Long) {
        val newIds = plan.added.mapTo(HashSet()) { it.id }
        val toSave = plan.added + plan.updated
        val ids = SavedIds()
        var done = 0
        toSave.chunked(BATCH_SIZE).forEach { batch ->
            library.inOneTransaction {
                batch.forEach { file ->
                    // In the same transaction as the save, so a song never has a new file id with old tags.
                    plan.moved[file.id]?.let { songId -> library.setSourceId(songId, file.id) }
                    save(file, ids, isNew = file.id in newIds)
                }
            }
            done += batch.size
            scanState.value = ScanState.Running(done = done, total = toSave.size)
        }
        library.markMissing(plan.gone, now)
        library.markMissing(plan.hidden, LibraryDao.HIDDEN_SINCE)
        library.setMissingSince(plan.lostWhileHidden, now)
        if (plan.updated.isNotEmpty() || plan.expired.isNotEmpty()) removeForGood(plan.expired)
    }

    /**
     * Removes the songs that have been gone for too long, and then the
     * albums, artists and folders that no song uses any more. Only a changed
     * or removed song can leave one of those behind. It runs last, because
     * an album saved for a song that is not saved yet would count as unused,
     * and in one transaction, so a scan that is stopped leaves no half.
     */
    private suspend fun removeForGood(songIds: List<Long>) = library.inOneTransaction {
        library.deleteSongs(songIds)
        library.deleteUnusedAlbums()
        library.deleteUnusedArtists()
        while (library.deleteUnusedFolders() > 0) {
            // A folder that held only empty folders is empty after the pass before.
        }
    }

    private suspend fun save(file: AudioFile, ids: SavedIds, isNew: Boolean) {
        val artist = file.artist.orEmpty()
        // The album belongs to the artist of the whole album, so a collection by many artists stays one album.
        val albumId = ids.album(file.album.orEmpty(), ids.artist(file.albumArtist ?: artist), file.year)
        // A file named only ".mp3" has no name before the ending; it shows its whole name.
        val title = file.title ?: file.fileName.substringBeforeLast('.').ifBlank { file.fileName }
        library.saveSong(
            SongEntity(
                source = SongSource.LOCAL,
                sourceId = file.id,
                title = title,
                titleKey = sortKey(title),
                artistId = ids.artist(artist),
                albumId = albumId,
                folderId = ids.folder(file.folderPath),
                fileName = file.fileName,
                mimeType = file.mimeType,
                durationMs = file.durationMs ?: 0,
                sizeBytes = file.sizeBytes,
                trackNumber = file.trackNumber,
                discNumber = file.discNumber,
                year = file.year,
                dateAdded = file.dateAdded,
                dateModified = file.dateModified,
            ),
            isNew = isNew,
        )
    }

    /**
     * The ids of the artists, albums and folders saved in this scan. Most
     * songs share them with the song before, so each is saved once for a
     * scan and not once for a song.
     */
    private inner class SavedIds {
        private val artists = HashMap<String, Long>()
        private val albums = HashMap<Triple<Long, String, Int?>, Long>()
        private val folders = HashMap<String, Long?>()

        suspend fun artist(name: String): Long =
            artists.getOrPut(sortKey(name)) { library.saveArtist(name, sortKey(name)) }

        /** The year is part of the key, so the first song that has a year gives it to its album. */
        suspend fun album(title: String, artistId: Long, year: Int?): Long =
            albums.getOrPut(Triple(artistId, sortKey(title), year)) {
                library.saveAlbum(title, sortKey(title), artistId, year)
            }

        /** Saves the folder with this path and every folder above it, and returns its id. No path gives no folder. */
        suspend fun folder(path: String): Long? = folders.getOrPut(path) {
            if (path.isEmpty()) {
                null
            } else {
                val parent = path.substringBeforeLast('/', missingDelimiterValue = "")
                library.saveFolder(path, name = path.substringAfterLast('/'), parentId = folder(parent))
            }
        }
    }

    private companion object {
        const val MS_PER_SECOND = 1000L

        /** Songs saved in one transaction. Large enough to be fast, small enough that the progress moves. */
        const val BATCH_SIZE = 500

        /** How long a song whose file is gone keeps its row: 30 days. */
        const val KEEP_MISSING_MS = 30L * 24 * 60 * 60 * 1000
    }
}
