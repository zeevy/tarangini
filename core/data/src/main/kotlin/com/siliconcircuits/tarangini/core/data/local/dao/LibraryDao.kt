/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.siliconcircuits.tarangini.core.data.local.FtsQuery
import com.siliconcircuits.tarangini.core.data.local.entity.FolderEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongEntity
import com.siliconcircuits.tarangini.core.data.local.entity.SongSource

/**
 * Reads and writes the library index: artists, albums, folders, songs and
 * the song search table.
 *
 * Every save is "insert, or update the row with the same natural key", so a
 * scan can run again and again and the ids stay the same. A save never
 * replaces a row, because that would remove the favourite, playlist and play
 * count rows that point to it.
 *
 * Lists leave out songs whose file is gone. Lists that can be long take a
 * limit and an offset. A list of ids of any length is written in parts of
 * [MAX_IDS], inside one transaction.
 */
@Dao
@Suppress("TooManyFunctions") // One function per library query; splitting the DAO would split its transactions.
internal abstract class LibraryDao {
    // Artists, albums and folders

    /** Saves the artist with this [nameKey] and returns its id. The first [name] saved for a key is kept. */
    @Transaction
    open suspend fun saveArtist(name: String, nameKey: String): Long {
        insertArtist(name, nameKey)
        return artistId(nameKey)
    }

    /**
     * Saves the album with this [titleKey] by this artist and returns its id.
     * The first [title] saved for a key is kept. A [year] replaces the stored
     * one; no year leaves it as it is.
     */
    @Transaction
    open suspend fun saveAlbum(title: String, titleKey: String, artistId: Long, year: Int?): Long {
        insertOrUpdateAlbum(title, titleKey, artistId, year)
        return albumId(artistId, titleKey)
    }

    /** Saves the folder with this [path] and returns its id. */
    @Transaction
    open suspend fun saveFolder(path: String, name: String, parentId: Long?): Long {
        insertOrUpdateFolder(path, name, parentId)
        return folderId(path)
    }

    @Query(
        """
            INSERT INTO artists(name, name_key) VALUES(:name, :nameKey)
            ON CONFLICT(name_key) DO NOTHING
        """,
    )
    protected abstract suspend fun insertArtist(name: String, nameKey: String)

    @Query("SELECT id FROM artists WHERE name_key = :nameKey")
    protected abstract suspend fun artistId(nameKey: String): Long

    @Query(
        """
            INSERT INTO albums(title, title_key, artist_id, year) VALUES(:title, :titleKey, :artistId, :year)
            ON CONFLICT(artist_id, title_key) DO UPDATE SET year = COALESCE(excluded.year, year)
        """,
    )
    protected abstract suspend fun insertOrUpdateAlbum(title: String, titleKey: String, artistId: Long, year: Int?)

    @Query("SELECT id FROM albums WHERE artist_id = :artistId AND title_key = :titleKey")
    protected abstract suspend fun albumId(artistId: Long, titleKey: String): Long

    @Query(
        """
            INSERT INTO folders(path, name, parent_id) VALUES(:path, :name, :parentId)
            ON CONFLICT(path) DO UPDATE SET name = excluded.name, parent_id = excluded.parent_id
        """,
    )
    protected abstract suspend fun insertOrUpdateFolder(path: String, name: String, parentId: Long?)

    @Query("SELECT id FROM folders WHERE path = :path")
    protected abstract suspend fun folderId(path: String): Long

    @Query(
        """
            SELECT al.*, ar.name AS artist_name,
            (SELECT COUNT(*) FROM songs s WHERE s.album_id = al.id AND s.missing_since IS NULL) AS song_count
            FROM albums al
            JOIN artists ar ON ar.id = al.artist_id
            WHERE EXISTS (SELECT 1 FROM songs s WHERE s.album_id = al.id AND s.missing_since IS NULL)
            ORDER BY al.title_key, al.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun albums(limit: Int, offset: Int): List<AlbumRow>

    @Query(
        """
            SELECT al.*, ar.name AS artist_name,
            (SELECT COUNT(*) FROM songs s WHERE s.album_id = al.id AND s.missing_since IS NULL) AS song_count
            FROM albums al
            JOIN artists ar ON ar.id = al.artist_id
            WHERE al.artist_id = :artistId
            AND EXISTS (SELECT 1 FROM songs s WHERE s.album_id = al.id AND s.missing_since IS NULL)
            ORDER BY al.title_key, al.id
        """,
    )
    abstract suspend fun albumsOfArtist(artistId: Long): List<AlbumRow>

    @Query(
        """
            SELECT ar.*,
            (SELECT COUNT(*) FROM songs s WHERE s.artist_id = ar.id AND s.missing_since IS NULL) AS song_count
            FROM artists ar
            WHERE EXISTS (SELECT 1 FROM songs s WHERE s.artist_id = ar.id AND s.missing_since IS NULL)
            ORDER BY ar.name_key, ar.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun artists(limit: Int, offset: Int): List<ArtistRow>

    /** The folders directly inside [parentId], or the top folders when it is null. */
    @Query("SELECT * FROM folders WHERE parent_id IS :parentId ORDER BY name, id")
    abstract suspend fun childFolders(parentId: Long?): List<FolderEntity>

    /**
     * Removes albums with no song. Returns how many were removed. A scan runs
     * the three clean up functions only after it has saved all its songs: an
     * album or artist saved for a song that is not saved yet counts as unused.
     */
    @Query("DELETE FROM albums WHERE id NOT IN (SELECT album_id FROM songs)")
    abstract suspend fun deleteUnusedAlbums(): Int

    /** Removes artists with no song and no album. Run it after [deleteUnusedAlbums]. */
    @Query(
        """
        DELETE FROM artists
        WHERE id NOT IN (SELECT artist_id FROM songs) AND id NOT IN (SELECT artist_id FROM albums)
        """,
    )
    abstract suspend fun deleteUnusedArtists(): Int

    /**
     * Removes folders with no song and no folder inside. A folder that held
     * only such folders is empty after one run, so run it until it returns 0.
     */
    @Query(
        """
        DELETE FROM folders
        WHERE id NOT IN (SELECT folder_id FROM songs WHERE folder_id IS NOT NULL)
            AND id NOT IN (SELECT parent_id FROM folders WHERE parent_id IS NOT NULL)
        """,
    )
    abstract suspend fun deleteUnusedFolders(): Int

    // Songs

    /**
     * Saves a song and its search row, and returns the song id. The song is
     * found by its source and source id; the id in [song] is not used. A song
     * that was marked as gone is back in the lists after this.
     *
     * Pass [isNew] only for a song that is known not to be stored: it has no
     * search row to remove, and a first scan saves a quarter of its
     * statements this way.
     */
    @Transaction
    open suspend fun saveSong(song: SongEntity, isNew: Boolean = false): Long {
        insertOrUpdateSong(song)
        val id = songId(song.source, song.sourceId)
        if (!isNew) deleteSearchRows(listOf(id))
        insertSearchRow(id)
        return id
    }

    private suspend fun insertOrUpdateSong(song: SongEntity) = with(song) {
        insertOrUpdateSong(
            source, sourceId, title, titleKey, artistId, albumId, folderId, fileName, mimeType,
            durationMs, sizeBytes, trackNumber, discNumber, year, dateAdded, dateModified,
        )
    }

    @Suppress("LongParameterList") // One parameter per column of the table.
    @Query(
        """
        INSERT INTO songs(
            source, source_id, title, title_key, artist_id, album_id, folder_id, file_name, mime_type,
            duration_ms, size_bytes, track_number, disc_number, year, date_added, date_modified, missing_since
        ) VALUES(
            :source, :sourceId, :title, :titleKey, :artistId, :albumId, :folderId, :fileName, :mimeType,
            :durationMs, :sizeBytes, :trackNumber, :discNumber, :year, :dateAdded, :dateModified, NULL
        )
        ON CONFLICT(source, source_id) DO UPDATE SET
            title = excluded.title, title_key = excluded.title_key, artist_id = excluded.artist_id,
            album_id = excluded.album_id, folder_id = excluded.folder_id, file_name = excluded.file_name,
            mime_type = excluded.mime_type, duration_ms = excluded.duration_ms, size_bytes = excluded.size_bytes,
            track_number = excluded.track_number, disc_number = excluded.disc_number, year = excluded.year,
            date_added = excluded.date_added, date_modified = excluded.date_modified, missing_since = NULL
        """,
    )
    protected abstract suspend fun insertOrUpdateSong(
        source: SongSource,
        sourceId: String,
        title: String,
        titleKey: String,
        artistId: Long,
        albumId: Long,
        folderId: Long?,
        fileName: String,
        mimeType: String,
        durationMs: Long,
        sizeBytes: Long,
        trackNumber: Int?,
        discNumber: Int?,
        year: Int?,
        dateAdded: Long,
        dateModified: Long,
    )

    @Query("SELECT id FROM songs WHERE source = :source AND source_id = :sourceId")
    protected abstract suspend fun songId(source: SongSource, sourceId: String): Long

    @Query(
        """
        INSERT INTO song_search(rowid, title, artist, album)
        SELECT s.id, s.title, ar.name, al.title
        FROM songs s
        JOIN artists ar ON ar.id = s.artist_id
        JOIN albums al ON al.id = s.album_id
        WHERE s.id = :songId
        """,
    )
    protected abstract suspend fun insertSearchRow(songId: Long)

    @Query("DELETE FROM song_search WHERE rowid IN (:songIds)")
    protected abstract suspend fun deleteSearchRows(songIds: List<Long>)

    /** Every stored song of [source], for a scan to compare with the files it finds. */
    @Query(
        """
        SELECT s.id, s.source_id, s.date_modified, s.missing_since, s.file_name, s.size_bytes, f.path AS folder_path
        FROM songs s
        LEFT JOIN folders f ON f.id = s.folder_id
        WHERE s.source = :source
        """,
    )
    abstract suspend fun songKeys(source: SongSource): List<SongKey>

    /**
     * Gives a stored song the new id of its file. The phone can give a file
     * a new id; the song keeps its row, so its favourite and playlist places
     * stay.
     */
    @Query("UPDATE songs SET source_id = :sourceId WHERE id = :songId")
    abstract suspend fun setSourceId(songId: Long, sourceId: String)

    /**
     * Runs [block] in one transaction. A scan saves a batch of songs with
     * their artists, albums and folders this way: one transaction for the
     * batch is many times faster than one for each row, and a batch is saved
     * as a whole or not at all.
     *
     * The block holds the one write connection of the database until it
     * ends. Call only functions of this DAO in it: no file, network or
     * settings read, and no wait.
     */
    @Transaction
    open suspend fun <T> inOneTransaction(block: suspend () -> T): T = block()

    /** Marks songs whose file is gone. A song that is already marked keeps its first time. */
    @Transaction
    open suspend fun markMissing(songIds: List<Long>, at: Long) {
        songIds.chunked(MAX_IDS).forEach { markMissingRows(it, at) }
    }

    @Query("UPDATE songs SET missing_since = :at WHERE id IN (:songIds) AND missing_since IS NULL")
    protected abstract suspend fun markMissingRows(songIds: List<Long>, at: Long)

    /**
     * Sets the time since when these songs are gone, also for songs that are
     * marked already. A scan uses it for a song that was only hidden by the
     * settings and whose file is now really gone: its keep time starts now.
     */
    @Transaction
    open suspend fun setMissingSince(songIds: List<Long>, at: Long) {
        songIds.chunked(MAX_IDS).forEach { setMissingSinceRows(it, at) }
    }

    @Query("UPDATE songs SET missing_since = :at WHERE id IN (:songIds)")
    protected abstract suspend fun setMissingSinceRows(songIds: List<Long>, at: Long)

    /**
     * Removes songs for good, with their search rows. Their favourite,
     * playlist, play count and queue rows go with them.
     */
    @Transaction
    open suspend fun deleteSongs(songIds: List<Long>) {
        songIds.chunked(MAX_IDS).forEach { ids ->
            deleteSearchRows(ids)
            deleteSongRows(ids)
        }
    }

    @Query("DELETE FROM songs WHERE id IN (:songIds)")
    protected abstract suspend fun deleteSongRows(songIds: List<Long>)

    /** The song with this id, also when its file is gone. */
    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.id = :songId
        """,
    )
    abstract suspend fun song(songId: Long): SongRow?

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY s.title_key, s.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun songsByTitle(limit: Int, offset: Int): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY ar.name_key, ar.id, s.title_key, s.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun songsByArtist(limit: Int, offset: Int): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY al.title_key, al.id, s.disc_number, s.track_number, s.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun songsByAlbum(limit: Int, offset: Int): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY s.date_added DESC, s.id DESC
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun songsByDateAdded(limit: Int, offset: Int): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.missing_since IS NULL
            ORDER BY s.duration_ms, s.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun songsByDuration(limit: Int, offset: Int): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.album_id = :albumId AND s.missing_since IS NULL
            ORDER BY s.disc_number, s.track_number, s.title_key, s.id
        """,
    )
    abstract suspend fun songsOfAlbum(albumId: Long): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.artist_id = :artistId AND s.missing_since IS NULL
            ORDER BY s.title_key, s.id
            LIMIT :limit OFFSET :offset
        """,
    )
    abstract suspend fun songsOfArtist(artistId: Long, limit: Int, offset: Int): List<SongRow>

    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM songs s
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE s.folder_id = :folderId AND s.missing_since IS NULL
            ORDER BY s.file_name, s.id
        """,
    )
    abstract suspend fun songsInFolder(folderId: Long): List<SongRow>

    /**
     * Songs whose title, artist or album matches [query]. Build the query
     * with [FtsQuery], never from raw text.
     */
    @Query(
        """
            SELECT s.*, ar.name AS artist_name, al.title AS album_title
            FROM song_search
            JOIN songs s ON s.id = song_search.rowid
            JOIN artists ar ON ar.id = s.artist_id
            JOIN albums al ON al.id = s.album_id
            WHERE song_search MATCH :query AND s.missing_since IS NULL
            ORDER BY s.title_key, s.id
            LIMIT :limit
        """,
    )
    abstract suspend fun searchSongs(query: String, limit: Int): List<SongRow>

    companion object {
        /** The most ids in one statement. SQLite limits the number of values a statement can take. */
        const val MAX_IDS = 500

        /**
         * The `missing_since` value of a song that the settings of the user
         * leave out while its file is still on the phone. It is later than
         * any real time, so such a song is never old enough to be removed.
         */
        const val HIDDEN_SINCE = Long.MAX_VALUE
    }
}
