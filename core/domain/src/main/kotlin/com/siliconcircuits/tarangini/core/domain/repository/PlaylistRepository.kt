/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong
import kotlinx.coroutines.flow.Flow

/**
 * The playlists of the person. A song whose file is gone keeps its place and is left out of the list
 * until the file is back. When the library drops the song for good, its places go with it.
 */
interface PlaylistRepository {
    /** Every playlist by name. A person has few, so the list comes whole. */
    suspend fun playlists(): List<Playlist>

    /** Fires at the start and after every change this app made to a playlist. */
    fun changes(): Flow<Unit>

    /** Makes a playlist and returns its id, or null when the name is not allowed (see checkPlaylistName). */
    suspend fun create(name: String): Long?

    /** Gives a playlist another name. False when the name is not allowed. */
    suspend fun rename(playlistId: Long, name: String): Boolean

    /** Takes a playlist away. Its songs stay in the library. */
    suspend fun delete(playlistId: Long)

    /** Puts a song at the end of a playlist. A song or a playlist that is gone is left alone. */
    suspend fun add(playlistId: Long, songId: Long)

    /** Adds the songs to the end of the playlist in the order given, as one change. A song may be in it twice. */
    suspend fun addAll(playlistId: Long, songIds: List<Long>)

    /** Takes one place out of a playlist. */
    suspend fun remove(itemId: Long)

    /** The songs of a playlist in its order. */
    suspend fun songs(playlistId: Long, page: Page): List<PlaylistSong>

    /** Moves a place so that it comes right before the place [beforeItemId], or to the end when that is null. */
    suspend fun move(playlistId: Long, itemId: Long, beforeItemId: Long?)
}
