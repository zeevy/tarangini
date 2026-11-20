/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query

/** Reads the folders for the settings, where a hidden folder must still be listed. */
@Dao
internal interface FolderDao {
    /** The path of every folder with a song row in it, also when its songs are hidden or their files are gone. */
    @Query(
        """
            SELECT DISTINCT f.path FROM folders f
            JOIN songs s ON s.folder_id = f.id
            ORDER BY f.path COLLATE NOCASE
        """,
    )
    suspend fun pathsWithSongs(): List<String>
}
