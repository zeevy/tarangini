/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.library

/**
 * Keeps the library in step with the files on the phone by itself, so new
 * and deleted songs show without an action of the user.
 *
 * A screen that wants a scan now, such as "Rescan" in Settings, calls
 * [LibraryScanner.scan]. The scanner runs one scan at a time, whoever asks.
 */
interface LibraryWatcher {
    /** Starts the watching. The app calls it once, when it starts. */
    fun start()
}
