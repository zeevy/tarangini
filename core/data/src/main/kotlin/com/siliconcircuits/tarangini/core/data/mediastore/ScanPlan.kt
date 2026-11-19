/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.data.local.dao.LibraryDao
import com.siliconcircuits.tarangini.core.data.local.dao.SongKey

/**
 * What one scan has to write, worked out before anything is written.
 *
 * @property added files that are not in the library
 * @property updated files that are in the library and changed, or came back
 * @property moved stored songs whose file got a new id from the phone: the new file id and the song id
 * @property gone ids of stored songs whose file was not found for the first time
 * @property hidden ids of stored songs whose file is on the phone but is now left out by the settings
 * @property lostWhileHidden ids of songs that were hidden and whose file is now gone: their keep time starts now
 * @property expired ids of stored songs whose file has been gone for longer than the keep time
 * @property unchanged how many files need no write
 */
internal data class ScanPlan(
    val added: List<AudioFile>,
    val updated: List<AudioFile>,
    val moved: Map<String, Long>,
    val gone: List<Long>,
    val hidden: List<Long>,
    val lostWhileHidden: List<Long>,
    val expired: List<Long>,
    val unchanged: Int,
) {
    /** True when the scan has nothing to write. */
    val isEmpty: Boolean
        get() = listOf(added, updated, gone, hidden, lostWhileHidden, expired).all { it.isEmpty() } && moved.isEmpty()
}

/**
 * Compares the stored songs with the files found now.
 *
 * The phone can give a file a new id, for example after a memory card was
 * taken out and put back. Such a file would look like a new song, and the
 * old song, with its favourite and playlist places, would look gone. So a
 * new file that has the folder, the name and the size of exactly one stored
 * song that was not found is taken as that song.
 *
 * A song whose file is on the phone but left out by the settings leaves the
 * lists and is never removed for good: a changed setting must not cost a
 * favourite. Its file is known by its id, or by its folder, name and size
 * when the phone gave it a new id in the meantime.
 *
 * @param found the files that belong in the library
 * @param hiddenFiles the files that are on the phone but left out by the settings
 * @param expiredBefore a song whose file has been gone since before this time is removed for good
 */
internal fun planScan(
    stored: List<SongKey>,
    found: List<AudioFile>,
    hiddenFiles: List<AudioFile>,
    expiredBefore: Long,
): ScanPlan {
    val storedById = stored.associateBy { it.sourceId }
    val foundIds = found.mapTo(HashSet()) { it.id }
    val (known, unknown) = found.partition { it.id in storedById }
    val hiddenIds = hiddenFiles.mapTo(HashSet()) { it.id }
    val hiddenPlaces = hiddenFiles.mapTo(HashSet()) { it.place() }
    val (hiddenRows, lostRows) = stored
        .filter { it.sourceId !in foundIds }
        .partition { it.sourceId in hiddenIds || it.place() in hiddenPlaces }

    val moved = matchMoved(lostRows, unknown)
    val movedSongIds = moved.values.toSet()
    val changed = known.filter { file ->
        val row = storedById.getValue(file.id)
        row.missingSince != null || row.dateModified != file.dateModified || row.sizeBytes != file.sizeBytes
    }
    val stillLost = lostRows.filter { it.id !in movedSongIds }

    return ScanPlan(
        added = unknown.filter { it.id !in moved },
        updated = changed + unknown.filter { it.id in moved },
        moved = moved,
        gone = stillLost.filter { it.missingSince == null }.map { it.id },
        hidden = hiddenRows.filter { it.missingSince == null }.map { it.id },
        lostWhileHidden = stillLost.filter { it.missingSince == LibraryDao.HIDDEN_SINCE }.map { it.id },
        expired = stillLost.filter { it.missingSince != null && it.missingSince < expiredBefore }.map { it.id },
        unchanged = known.size - changed.size,
    )
}

/** Where a file is and how large it is: what stays the same when the phone gives the file a new id. */
private fun AudioFile.place() = Triple(folderPath, fileName, sizeBytes)

private fun SongKey.place() = Triple(folderPath.orEmpty(), fileName, sizeBytes)

/** Pairs a lost song with the new file in the same folder with the same name and size, when there is one of each. */
private fun matchMoved(lost: List<SongKey>, unknown: List<AudioFile>): Map<String, Long> {
    val songsByPlace = lost.groupBy { it.place() }
    val filesByPlace = unknown.groupBy { it.place() }
    return songsByPlace.mapNotNull { (place, songs) ->
        val files = filesByPlace[place]
        if (songs.size == 1 && files?.size == 1) files.single().id to songs.single().id else null
    }.toMap()
}
