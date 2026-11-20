/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.mediastore

import com.siliconcircuits.tarangini.core.domain.library.LibraryScanner
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a scan that nobody waits for: the scan at the start, after a change
 * of the files, and in the rescan job. An error in such a scan, for example
 * a full disk, must not end the app. The next scan tries again.
 */
internal suspend fun LibraryScanner.scanQuietly() {
    runCatching { scan() }.onFailure { if (it is CancellationException) throw it }
}
