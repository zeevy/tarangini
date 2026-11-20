/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.repository

import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistDao
import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistRow
import com.siliconcircuits.tarangini.core.data.local.entity.PlaylistEntity
import com.siliconcircuits.tarangini.core.data.mapper.toSong
import com.siliconcircuits.tarangini.core.data.mediastore.TimeSource
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Playlist
import com.siliconcircuits.tarangini.core.domain.model.PlaylistSong
import com.siliconcircuits.tarangini.core.domain.model.checkPlaylistName
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart

/** The playlists from the database of the app. One object for the app, so every screen hears a change. */
@Singleton
internal class DefaultPlaylistRepository @Inject constructor(
    private val playlists: PlaylistDao,
    private val time: TimeSource,
) : PlaylistRepository {
    private val changed =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun playlists(): List<Playlist> = playlists.playlists().map { it.toPlaylist() }

    override fun changes(): Flow<Unit> = changed.onStart { emit(Unit) }

    override suspend fun create(name: String): Long? = checkPlaylistName(name)?.let { checked ->
        val now = time.nowMs()
        playlists.insertPlaylist(PlaylistEntity(name = checked, createdAt = now, updatedAt = now)).also { tell() }
    }

    override suspend fun rename(playlistId: Long, name: String): Boolean {
        val checked = checkPlaylistName(name) ?: return false
        playlists.renamePlaylist(playlistId, checked, time.nowMs())
        tell()
        return true
    }

    override suspend fun delete(playlistId: Long) {
        playlists.deletePlaylist(playlistId)
        tell()
    }

    override suspend fun add(playlistId: Long, songId: Long) {
        playlists.addItem(playlistId, songId, time.nowMs())
        tell()
    }

    override suspend fun remove(itemId: Long) {
        playlists.removeItem(itemId)
        tell()
    }

    override suspend fun songs(playlistId: Long, page: Page): List<PlaylistSong> =
        playlists.items(playlistId, page.limit, page.offset).map { PlaylistSong(it.itemId, it.row.toSong()) }

    override suspend fun move(playlistId: Long, itemId: Long, beforeItemId: Long?) {
        val order = playlists.itemIds(playlistId)
        if (itemId !in order || itemId == beforeItemId) return
        val rest = order - itemId
        // A place that is gone in the meantime counts as "the end".
        val at = rest.indexOf(beforeItemId).takeIf { it >= 0 } ?: rest.size
        playlists.setItemOrder(playlistId, rest.take(at) + itemId + rest.drop(at))
        tell()
    }

    private fun tell() {
        changed.tryEmit(Unit)
    }
}

private fun PlaylistRow.toPlaylist() = Playlist(playlist.id, playlist.name, songCount)
