/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.siliconcircuits.tarangini.core.data.local.entity.QueueItemEntity
import com.siliconcircuits.tarangini.core.data.local.entity.ResumeStateEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource

/** Reads and writes the saved play queue and the place where playback stopped. */
@Dao
internal abstract class QueueDao {
    /** Saves [songIds] as the whole queue, in this order, in place of the stored one. */
    @Transaction
    open suspend fun replaceQueue(songIds: List<Long>) {
        clearQueue()
        insertItems(songIds.mapIndexed { index, songId -> QueueItemEntity(position = index, songId = songId) })
    }

    @Query("DELETE FROM queue_items")
    abstract suspend fun clearQueue()

    @Insert
    protected abstract suspend fun insertItems(items: List<QueueItemEntity>)

    /**
     * The saved queue in order as file ids, without songs whose file is
     * gone. It reads two columns only, so a very long queue stays cheap.
     * The resume state names a song by [QueueFileRow.position], not by its
     * place in this list.
     */
    @Query(
        """
            SELECT q.position, s.source_id
            FROM queue_items q
            JOIN songs s ON s.id = q.song_id
            WHERE s.missing_since IS NULL AND s.source = :source
            ORDER BY q.position
        """,
    )
    abstract suspend fun queueFiles(source: SongSource): List<QueueFileRow>

    /** The row id and the file id of every song of one source, to find the song rows of a queue. */
    @Query("SELECT id, source_id FROM songs WHERE source = :source")
    abstract suspend fun localSongIds(source: SongSource): List<SongIdRow>

    /** Saves the queue and the place in it together, so the place never names an item of another queue. */
    @Transaction
    open suspend fun saveQueue(songIds: List<Long>, state: ResumeStateEntity) {
        replaceQueue(songIds)
        saveResumeState(state)
    }

    /**
     * Runs [block] in one transaction. The block holds the one write
     * connection of the database until it ends. Call only functions of this
     * DAO in it, and no wait.
     */
    @Transaction
    open suspend fun <T> inOneTransaction(block: suspend () -> T): T = block()

    @Upsert
    abstract suspend fun saveResumeState(state: ResumeStateEntity)

    @Query("SELECT * FROM resume_state WHERE id = ${ResumeStateEntity.SINGLE_ROW_ID}")
    abstract suspend fun resumeState(): ResumeStateEntity?
}
