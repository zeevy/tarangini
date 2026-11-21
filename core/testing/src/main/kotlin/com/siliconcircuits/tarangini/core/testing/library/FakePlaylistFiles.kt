/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.repository.PlaylistFiles
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistImport

/** Playlist files for tests: a test sets how an import and an export end, and reads what was asked. */
class FakePlaylistFiles : PlaylistFiles {
    /** How the next import ends. */
    var importResult: PlaylistImport = PlaylistImport.NothingFound

    /** False makes the next export fail. */
    var exportWorks = true

    /** Every call, as "export <playlist id> <address>" or "import <address>". */
    val calls = mutableListOf<String>()

    override suspend fun exportTo(playlistId: Long, target: String): Boolean {
        calls += "export $playlistId $target"
        return exportWorks
    }

    override suspend fun importFrom(source: String): PlaylistImport {
        calls += "import $source"
        return importResult
    }
}
