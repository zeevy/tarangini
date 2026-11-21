/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.settings

import com.siliconcircuits.tarangini.core.domain.repository.BackupFiles
import com.siliconcircuits.tarangini.core.domain.repository.BackupRestore
import kotlinx.coroutines.CompletableDeferred

/** Backups for tests: a test sets how a backup and a restore end, and reads what was asked. */
class FakeBackupFiles : BackupFiles {
    /** How the next restore ends. */
    var restoreResult: BackupRestore = BackupRestore.Done(favourites = 0, playlists = 0, songsNotFound = 0)

    /** False makes the next backup fail. */
    var backupWorks = true

    /** A restore waits for this when it is set, so a test can look at the time in between. */
    var restoreWaitsFor: CompletableDeferred<Unit>? = null

    /** Every call, as "backup <address>" or "restore <address> replace=<true or false>". */
    val calls = mutableListOf<String>()

    override suspend fun backUpTo(target: String): Boolean {
        calls += "backup $target"
        return backupWorks
    }

    override suspend fun restoreFrom(source: String, replace: Boolean): BackupRestore {
        calls += "restore $source replace=$replace"
        restoreWaitsFor?.await()
        return restoreResult
    }
}
