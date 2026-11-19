/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.library

import kotlinx.coroutines.flow.StateFlow

/**
 * Copies the songs on the phone into the library of the app and keeps the
 * copy in step: new files are added, changed files are read again and files
 * that are gone leave the lists.
 *
 * Favourites, playlists and play counts survive a scan.
 */
interface LibraryScanner {
    /** What the scanner is doing now. A screen shows progress and the last result from this. */
    val state: StateFlow<ScanState>

    /**
     * Runs one scan and returns when it is done. Only one scan runs at a
     * time: a second call waits for the first and then scans again.
     */
    suspend fun scan(): ScanOutcome
}

/** The state of the scanner. */
sealed interface ScanState {
    /** No scan has run since the app started, or the last one was stopped before it ended. */
    data object Idle : ScanState

    /**
     * A scan runs: [done] of [total] changed files are saved. Both are 0
     * while the scan still reads the file list of the phone.
     */
    data class Running(val done: Int, val total: Int) : ScanState

    /** The last scan ended with [outcome]. */
    data class Finished(val outcome: ScanOutcome) : ScanState
}

/** How a scan ended. A failure is a value, because a missing permission is no bug. */
sealed interface ScanOutcome {
    data class Success(val summary: ScanSummary) : ScanOutcome

    data class Failure(val reason: ScanFailure) : ScanOutcome
}

/** Why a scan could not run. */
enum class ScanFailure {
    /** The app may not read the audio files of the phone. */
    NO_PERMISSION,

    /** The phone gave no answer about its files, for example while its storage is not ready. */
    STORAGE_UNAVAILABLE,
}

/**
 * What one scan changed, in files.
 *
 * @property added files that were not in the library
 * @property updated files whose tags or content changed
 * @property removed files that are gone and left the lists
 * @property unchanged files the scan did not have to write
 */
data class ScanSummary(val added: Int, val updated: Int, val removed: Int, val unchanged: Int)
