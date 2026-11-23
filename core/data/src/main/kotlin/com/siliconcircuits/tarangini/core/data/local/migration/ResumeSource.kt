/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 3 to 4: the saved queue keeps the list it came from, such as an album.
 *
 * Both columns may be empty, so a queue saved by an older version comes back with no source and
 * now playing shows "Now playing" for it, as before.
 */
internal object ResumeSource : Migration(FROM_VERSION, TO_VERSION) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `resume_state` ADD COLUMN `source_kind` TEXT")
        connection.execSQL("ALTER TABLE `resume_state` ADD COLUMN `source_name` TEXT")
    }
}

private const val FROM_VERSION = 3
private const val TO_VERSION = 4
