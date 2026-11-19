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
     * The saved queue in order, without songs whose file is gone. The resume
     * state names a song by [QueueSongRow.position], not by its place in
     * this list.
     */
    @Query(
        """
            SELECT q.position, s.*, ar.name AS artist_name, al.title AS album_title
            FROM queue_items q
            JOIN songs s ON s.id = q.song_id
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY q.position
        """,
    )
    abstract suspend fun queue(): List<QueueSongRow>

    @Upsert
    abstract suspend fun saveResumeState(state: ResumeStateEntity)

    @Query("SELECT * FROM resume_state WHERE id = ${ResumeStateEntity.SINGLE_ROW_ID}")
    abstract suspend fun resumeState(): ResumeStateEntity?
}
