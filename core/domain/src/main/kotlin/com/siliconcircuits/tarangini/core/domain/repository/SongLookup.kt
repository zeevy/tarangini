/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Song

/**
 * Finds songs by the address they play from. The playback service uses it
 * to give the items of the queue their title, artist and album.
 */
interface SongLookup {
    /**
     * The songs that play from these addresses, each under its address, also when its file is gone.
     * An address the library does not know is left out. The list may be as long as a queue.
     */
    suspend fun byMediaUri(mediaUris: List<String>): Map<String, Song>
}
