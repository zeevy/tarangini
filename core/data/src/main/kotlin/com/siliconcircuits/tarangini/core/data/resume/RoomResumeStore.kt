/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.resume

import android.net.Uri
import android.provider.MediaStore
import com.siliconcircuits.tarangini.core.data.di.IoDispatcher
import com.siliconcircuits.tarangini.core.data.local.dao.QueueDao
import com.siliconcircuits.tarangini.core.data.local.entity.ResumeStateEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource
import com.siliconcircuits.tarangini.core.data.local.entity.StoredRepeatMode
import com.siliconcircuits.tarangini.core.data.mediastore.localMediaUri
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.QueueSourceKind
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.ResumeStore
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Keeps the queue and the place in it in the database.
 *
 * The player knows a song by the address of its file in MediaStore. The
 * database stores the song row, so a song that is removed from the library
 * leaves the saved queue by itself. An item that is not a song of the
 * library is not saved.
 *
 * A station is not saved yet: the app cannot play one. When it can, the
 * station goes into `resume_state.station_id`.
 */
@Singleton
internal class RoomResumeStore @Inject constructor(
    private val queue: QueueDao,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ResumeStore {
    /**
     * For each place in the player's queue, the place of that item in the
     * stored queue. An item that was not saved has the place of the next
     * stored item, written as `-place - 1`. Null until a queue was saved or
     * read since the app started.
     */
    @Volatile
    private var storedPlaces: IntArray? = null

    /** The source of the stored queue. A save of the place alone keeps it. */
    @Volatile
    private var storedSource: QueueSource? = null

    override suspend fun saveQueue(playback: SavedPlayback) = withContext(io) {
        val places = IntArray(playback.mediaIds.size)
        // One transaction: a library scan cannot remove a song between the read of the ids and the save.
        queue.inOneTransaction {
            val songIds = queue.localSongIds(SongSource.LOCAL).associate { it.sourceId to it.id }
            val kept = ArrayList<Long>(playback.mediaIds.size)
            playback.mediaIds.forEachIndexed { index, mediaId ->
                val songId = sourceIdOf(mediaId)?.let(songIds::get)
                places[index] = if (songId == null) -kept.size - 1 else kept.size
                songId?.let(kept::add)
            }
            queue.saveQueue(kept, playback.place.toEntity(places, playback.source))
        }
        storedPlaces = places
        storedSource = playback.source
    }

    override suspend fun savePlace(place: ResumePlace) {
        // Without a queue saved in this run, the stored queue is the one of the last run, and a place in the
        // player's queue says nothing about it.
        val places = storedPlaces ?: return
        withContext(io) { queue.saveResumeState(place.toEntity(places, storedSource)) }
    }

    override suspend fun load(): SavedPlayback? = withContext(io) {
        val state = queue.resumeState()
        val files = queue.queueFiles(SongSource.LOCAL).filter { it.sourceId.toLongOrNull() != null }
        // The player is filled from this list, so a place in the player is a place in this list.
        storedPlaces = IntArray(files.size) { files[it].position }
        storedSource = state?.source()
        if (state == null || state.stationId != null || files.isEmpty()) return@withContext null

        // A song whose file is gone is not in the list. The next song that is there takes its place, from its start.
        val found = files.indexOfFirst { it.position >= state.queuePosition }
        val index = if (found < 0) 0 else found
        val samePlace = found >= 0 && files[found].position == state.queuePosition
        SavedPlayback(
            mediaIds = files.map { localMediaUri(it.sourceId) },
            place = ResumePlace(
                index = index,
                positionMs = if (samePlace) state.positionMs else 0,
                shuffle = state.shuffle,
                repeat = RepeatMode.valueOf(state.repeatMode.name),
            ),
            source = storedSource,
        )
    }

    private fun ResumePlace.toEntity(places: IntArray, source: QueueSource?): ResumeStateEntity {
        val place = places.getOrElse(index) { 0 }
        // The current item is not stored: the next stored item takes its place, from its start.
        val notStored = place < 0
        return ResumeStateEntity(
            queuePosition = if (notStored) -place - 1 else place,
            positionMs = if (notStored) 0 else positionMs.coerceAtLeast(0),
            stationId = null,
            shuffle = shuffle,
            repeatMode = StoredRepeatMode.valueOf(repeat.name),
            sourceKind = source?.kind?.name,
            sourceName = source?.name,
        )
    }

    /** The stored source, or null when there is none or its kind is not known to this version. */
    private fun ResumeStateEntity.source(): QueueSource? =
        QueueSourceKind.entries.firstOrNull { it.name == sourceKind }?.let { QueueSource.of(it, sourceName.orEmpty()) }

    private companion object {
        /** The id MediaStore gave the file: the number at the end of `content://media/<volume>/audio/media/<n>`. */
        fun sourceIdOf(mediaId: String): String? {
            val uri = Uri.parse(mediaId)
            val number = uri.lastPathSegment.orEmpty()
            val isMediaStoreItem =
                uri.authority == MediaStore.AUTHORITY && number.isNotEmpty() && number.all(Char::isDigit)
            return number.takeIf { isMediaStoreItem }
        }
    }
}
