/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.model

/**
 * What the person chose to leave out of the library.
 *
 * @property minTrackSeconds files shorter than this are left out; 0 leaves none out
 * @property hiddenFolders the folders whose files are left out, with the folders inside them
 */
data class ScanSettings(val minTrackSeconds: Int = 0, val hiddenFolders: Set<String> = emptySet())

/** The longest "shortest track" that can be set: ten minutes. */
const val MAX_MIN_TRACK_SECONDS = 600
