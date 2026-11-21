/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.repository

/**
 * A backup of what the person made in the app, as one file: the favourites, the playlists and the
 * settings of the library. The file is written and read through an address of the file picker of
 * the system. A song is named in the file by where its file is, so a backup can be read on another
 * phone that holds the same music.
 */
interface BackupFiles {
    /** Writes the backup to [target]. False when nothing could be written. */
    suspend fun backUpTo(target: String): Boolean

    /**
     * Reads the backup at [source]. The file is not trusted: it is checked before anything changes.
     *
     * @param replace false adds what the file holds to what is here; true also takes away the
     * favourites, playlists and hidden folders that the file does not hold
     */
    suspend fun restoreFrom(source: String, replace: Boolean): BackupRestore
}

/** How the reading of a backup ended. */
sealed interface BackupRestore {
    /**
     * The backup was read.
     *
     * @property favourites how many favourites of the file are songs of the library
     * @property playlists how many playlists were made
     * @property songsNotFound how many songs the file names that the library does not have
     */
    data class Done(val favourites: Int, val playlists: Int, val songsNotFound: Int) : BackupRestore

    /** The file is larger than a backup can be. Nothing was read. */
    data object TooLarge : BackupRestore

    /** The file is no backup of this app. Nothing was changed. */
    data object NotABackup : BackupRestore

    /** The file was written by a newer version of the app. Nothing was changed. */
    data object NewerVersion : BackupRestore

    /** The library has no song yet, so no song of the file can be found. Nothing was changed. */
    data object NoLibrary : BackupRestore

    /** The file could not be read. */
    data object Failed : BackupRestore
}
