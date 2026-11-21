/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.playlist

import com.siliconcircuits.tarangini.core.domain.repository.PlaylistFiles
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistImport
import com.siliconcircuits.tarangini.feature.library.list.attempt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The import of a playlist file for a screen: it runs in [scope] and keeps how it ended until the
 * person has read it.
 */
internal class PlaylistFileImport(private val scope: CoroutineScope, private val files: PlaylistFiles) {
    private val ended = MutableStateFlow<PlaylistImport?>(null)

    /** How the last import ended, or null when there is nothing to tell. */
    val result: StateFlow<PlaylistImport?> = ended.asStateFlow()

    /** Reads the file the person picked. [address] is what the file picker gave. */
    fun from(address: String) {
        scope.launch { ended.value = attempt { files.importFrom(address) }.getOrDefault(PlaylistImport.Failed) }
    }

    /** The person has read the result. */
    fun close() {
        ended.value = null
    }
}

/** The export of one playlist for a screen, in the same way as [PlaylistFileImport]. */
internal class PlaylistFileExport(
    private val scope: CoroutineScope,
    private val files: PlaylistFiles,
    private val playlistId: Long,
) {
    private val ended = MutableStateFlow<Boolean?>(null)

    /** True after a file was written, false after a failure, null when there is nothing to tell. */
    val result: StateFlow<Boolean?> = ended.asStateFlow()

    /** Writes the playlist to the file the person picked. */
    fun to(address: String) {
        scope.launch { ended.value = attempt { files.exportTo(playlistId, address) }.getOrDefault(false) }
    }

    /** The person has read the result. */
    fun close() {
        ended.value = null
    }
}

/**
 * What a screen shows and does for playlist files.
 *
 * @property imported how an import ended, while the person has not read it yet
 * @property exported the same for an export: true for a file that was written
 * @property onImport opens the file picker to read a file
 * @property onExport opens the file picker to write the playlist
 * @property onClose the person has read the result
 */
internal class PlaylistFileActions(
    val imported: PlaylistImport? = null,
    val exported: Boolean? = null,
    val onImport: () -> Unit = {},
    val onExport: () -> Unit = {},
    val onClose: () -> Unit = {},
)
