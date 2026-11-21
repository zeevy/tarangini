/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.repository.SongLyrics
import kotlinx.coroutines.CompletableDeferred

/** Lyrics for tests: a test says which address has which lyrics. */
class FakeSongLyrics : SongLyrics {
    /** The lyrics by the address of the song. An address that is not here has none. */
    val lyrics = mutableMapOf<String, String>()

    /** A read waits for this when it is set, so a test can look at the time in between. */
    var waitsFor: CompletableDeferred<Unit>? = null

    /** Every address that was asked for, in order. */
    val asked = mutableListOf<String>()

    override suspend fun lyricsOf(mediaUri: String): String? {
        asked += mediaUri
        waitsFor?.await()
        return lyrics[mediaUri]
    }
}
