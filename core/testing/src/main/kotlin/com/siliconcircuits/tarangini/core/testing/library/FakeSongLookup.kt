/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup

/**
 * A [SongLookup] that finds the songs a test gives it.
 *
 * @property failure when set, a read fails with it, the way a closed database does
 */
class FakeSongLookup(var songs: List<Song> = emptyList(), var failure: Exception? = null) : SongLookup {
    override suspend fun byMediaUri(mediaUris: List<String>): Map<String, Song> {
        failure?.let { throw it }
        val wanted = mediaUris.toSet()
        return songs.filter { it.mediaUri in wanted }.associateBy { it.mediaUri }
    }
}
