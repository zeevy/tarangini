/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.siliconcircuits.swaratarang.core.data.local.entity.FavouriteSongEntity
import com.siliconcircuits.swaratarang.core.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes favourite songs and favourite stations. */
@Dao
internal abstract class FavouriteDao {
    /** Marks a song as a favourite. A song that is one already keeps its first time. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addSong(favourite: FavouriteSongEntity)

    @Query("DELETE FROM favourite_songs WHERE song_id = :songId")
    abstract suspend fun removeSong(songId: Long)

    @Query("SELECT EXISTS (SELECT 1 FROM favourite_songs WHERE song_id = :songId)")
    abstract fun isFavouriteSong(songId: Long): Flow<Boolean>

    /** Favourite songs, newest first. */
    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM favourite_songs f
            JOIN songs s ON s.id = f.song_id
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY f.added_at DESC, f.song_id DESC
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun favouriteSongs(limit: Int, offset: Int): List<SongRow>

    /**
     * Adds a station at the end of the favourites. One statement, so two adds
     * at once cannot get the same place.
     */
    @Query(
        """
            INSERT OR IGNORE INTO favourite_stations(station_id, position)
            SELECT :stationId, COALESCE(MAX(position), -1) + 1 FROM favourite_stations
        """,
    )
    abstract suspend fun addStation(stationId: Long)

    @Query("DELETE FROM favourite_stations WHERE station_id = :stationId")
    abstract suspend fun removeStation(stationId: Long)

    @Query("SELECT EXISTS (SELECT 1 FROM favourite_stations WHERE station_id = :stationId)")
    abstract fun isFavouriteStation(stationId: Long): Flow<Boolean>

    /** Favourite stations in the order the user set. */
    @Query(
        """
            SELECT st.* FROM favourite_stations f
            JOIN stations st ON st.id = f.station_id
            ORDER BY f.position, f.station_id
        """,
    )
    abstract suspend fun favouriteStations(): List<StationEntity>

    /** Puts the favourite stations in the order of [stationIds]. */
    @Transaction
    open suspend fun setStationOrder(stationIds: List<Long>) {
        stationIds.forEachIndexed { index, stationId -> setStationPosition(stationId, index) }
    }

    @Query("UPDATE favourite_stations SET position = :position WHERE station_id = :stationId")
    protected abstract suspend fun setStationPosition(stationId: Long, position: Int)
}
