/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import com.siliconcircuits.tarangini.core.domain.repository.BackupFiles
import com.siliconcircuits.tarangini.core.domain.repository.BackupRestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the backup rows of Settings have to tell.
 *
 * @property saved true after a backup was written, false after a failure, null when there is nothing to tell
 * @property restored how a restore ended, while the person has not read it yet
 * @property busy true while a backup is written or read
 */
internal data class BackupUiState(
    val saved: Boolean? = null,
    val restored: BackupRestore? = null,
    val busy: Boolean = false,
)

/** Writes a backup to a file and reads one back. */
@HiltViewModel
internal class BackupViewModel @Inject constructor(
    private val files: BackupFiles,
    private val scanner: LibraryScanner,
) : ViewModel() {
    private val shown = MutableStateFlow(BackupUiState())

    val state: StateFlow<BackupUiState> = shown.asStateFlow()

    /** Writes the backup to the file the person picked. [address] is what the file picker gave. */
    fun backUpTo(address: String) = oneAtATime {
        shown.value = BackupUiState(saved = attempt { files.backUpTo(address) } ?: false)
    }

    /** Reads the backup the person picked, then scans: the settings of the file can hide or show songs. */
    fun restoreFrom(address: String, replace: Boolean) = oneAtATime {
        val result = attempt { files.restoreFrom(address, replace) } ?: BackupRestore.Failed
        shown.value = BackupUiState(restored = result)
        if (result is BackupRestore.Done) attempt { scanner.scan() }
    }

    /** The person has read the result. */
    fun close() {
        shown.value = BackupUiState()
    }

    /**
     * Two restores at the same time would each make the playlists of their file, or one would
     * remove what the other made. So a call while another one runs does nothing.
     */
    private fun oneAtATime(work: suspend () -> Unit) {
        if (shown.value.busy) return
        shown.value = BackupUiState(busy = true)
        viewModelScope.launch { work() }
    }
}

/** The result, or null after a failure. A cancelled coroutine stays cancelled. */
private suspend fun <T> attempt(block: suspend () -> T): T? =
    runCatching { block() }.onFailure { if (it is CancellationException) throw it }.getOrNull()
