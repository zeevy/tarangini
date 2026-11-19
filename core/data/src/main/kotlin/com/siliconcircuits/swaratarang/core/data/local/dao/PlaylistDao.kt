/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.siliconcircuits.swaratarang.core.data.local.entity.PlaylistEntity

/** Reads and writes playlists and the songs in them. */
@Dao
internal abstract class PlaylistDao {
    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updated_at = :at WHERE id = :playlistId")
    abstract suspend fun renamePlaylist(playlistId: Long, name: String, at: Long)

    /** Removes a playlist and its places. The songs stay in the library. */
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    abstract suspend fun deletePlaylist(playlistId: Long)

    /** Every playlist by name. A user has few playlists, so the list is not paged. */
    @Query(
        """
            SELECT p.*,
            (
            SELECT COUNT(*) FROM playlist_items i
            JOIN songs s ON s.id = i.song_id
            WHERE i.playlist_id = p.id AND s.missing_since IS NULL
            ) AS song_count
            FROM playlists p
            ORDER BY p.name COLLATE NOCASE, p.id
        """,
    )
    abstract suspend fun playlists(): List<PlaylistRow>

    /**
     * Adds a song at the end of a playlist and returns the id of its place.
     * One statement, so two adds at once cannot get the same place.
     */
    @Query(
        """
            INSERT INTO playlist_items(playlist_id, song_id, position, added_at)
            SELECT :playlistId, :songId, COALESCE(MAX(position), -1) + 1, :at
            FROM playlist_items WHERE playlist_id = :playlistId
        """,
    )
    abstract suspend fun addItem(playlistId: Long, songId: Long, at: Long): Long

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    abstract suspend fun removeItem(itemId: Long)

    /**
     * The songs of a playlist in its order. A song whose file is gone keeps
     * its place and is left out until the file is back.
     */
    @Query(
        """
            SELECT i.id AS item_id, s.*, ar.name AS artist_name, al.title AS album_title
            FROM playlist_items i
            JOIN songs s ON s.id = i.song_id
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE i.playlist_id = :playlistId AND s.missing_since IS NULL
            ORDER BY i.position, i.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun items(playlistId: Long, limit: Int, offset: Int): List<PlaylistSongRow>

    /** Puts the places of a playlist in the order of [itemIds]. */
    @Transaction
    open suspend fun setItemOrder(playlistId: Long, itemIds: List<Long>) {
        itemIds.forEachIndexed { index, itemId -> setItemPosition(playlistId, itemId, index) }
    }

    @Query("UPDATE playlist_items SET position = :position WHERE id = :itemId AND playlist_id = :playlistId")
    protected abstract suspend fun setItemPosition(playlistId: Long, itemId: Long, position: Int)
}
