/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.ColumnInfo

/** What a scan needs to know about a stored song to tell new, changed and gone files apart. */
internal data class SongKey(
    val id: Long,
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "missing_since") val missingSince: Long?,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    /** The path of the folder the file was in, or null when it had none. */
    @ColumnInfo(name = "folder_path") val folderPath: String?,
)
