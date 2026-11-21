/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.library

import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.checkPlaylistName
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Playlists kept in memory. A test sets [known] to the songs of its library. */
class FakePlaylistRepository : PlaylistRepository {
    /** The songs that can be shown. A place of another song counts as a song whose file is gone. */
    var known: List<Song> = emptyList()

    /** The names by playlist id. */
    val names = linkedMapOf<Long, String>()

    /** The places of each playlist in order, as item id to song id. */
    val places = mutableMapOf<Long, List<Pair<Long, Long>>>()

    private val version = MutableStateFlow(0)

    /** The places of a playlist whose song can be shown. */
    private val visible: (Long) -> List<PlaylistSong> = { playlistId ->
        places[playlistId].orEmpty().mapNotNull { (item, songId) ->
            known.firstOrNull { it.id == songId }?.let { PlaylistSong(item, it) }
        }
    }
    private var nextId = 1L

    /** Makes a change and tells the readers. */
    private val change: (() -> Unit) -> Unit = { block ->
        block()
        version.value++
    }

    override suspend fun playlists(): List<Playlist> =
        names.map { (id, name) -> Playlist(id, name, visible(id).size) }.sortedBy { it.name.lowercase() }

    override fun changes(): Flow<Unit> = version.map { }

    override suspend fun create(name: String): Long? = checkPlaylistName(name)?.let { checked ->
        (nextId++).also { id -> change { names[id] = checked } }
    }

    override suspend fun rename(playlistId: Long, name: String): Boolean {
        val checked = checkPlaylistName(name) ?: return false
        change { if (playlistId in names) names[playlistId] = checked }
        return true
    }

    override suspend fun delete(playlistId: Long) = change {
        names.remove(playlistId)
        places.remove(playlistId)
    }

    override suspend fun add(playlistId: Long, songId: Long) = change {
        if (playlistId in names) places[playlistId] = places[playlistId].orEmpty() + (nextId++ to songId)
    }

    override suspend fun addAll(playlistId: Long, songIds: List<Long>) = change {
        if (playlistId in names) places[playlistId] = places[playlistId].orEmpty() + songIds.map { nextId++ to it }
    }

    override suspend fun remove(itemId: Long) = change {
        places.replaceAll { _, list -> list.filterNot { it.first == itemId } }
    }

    override suspend fun songs(playlistId: Long, page: Page): List<PlaylistSong> =
        visible(playlistId).drop(page.offset).take(page.limit)

    override suspend fun move(playlistId: Long, itemId: Long, beforeItemId: Long?) = change {
        val order = places[playlistId].orEmpty()
        val moved = order.firstOrNull { it.first == itemId } ?: return@change
        val rest = order - moved
        val at = rest.indexOfFirst { it.first == beforeItemId }.takeIf { it >= 0 } ?: rest.size
        places[playlistId] = rest.take(at) + moved + rest.drop(at)
    }
}
