/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.backup

import com.siliconcircuits.tarangini.core.data.local.dao.BackupDao
import com.siliconcircuits.tarangini.core.data.playlist.SongFileFinder
import com.siliconcircuits.tarangini.core.domain.repository.BackupRestore
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** What a backup reads from and writes to. */
internal class BackupSources @Inject constructor(
    val backup: BackupDao,
    val playlists: PlaylistRepository,
    val settings: SettingsRepository,
)

/**
 * Puts what a backup holds into the app.
 *
 * With "replace", what the file holds is added first and what it does not hold is taken away last.
 * So a failure on the way leaves more than was asked for, and never less than was there.
 */
internal class BackupRestorer(private val sources: BackupSources) {
    // A song whose file is gone for now is found too. Else "replace" would take its mark away.
    private val finder = SongFileFinder(sources.backup::filesNamed)

    /** @param now the time of the restore, in milliseconds */
    suspend fun restore(content: BackupContent, replace: Boolean, now: Long): BackupRestore.Done {
        val named = (content.favourites + content.playlists.flatMap { it.songs }).distinct()
        val songIds: Map<String, Long?> = named.zip(finder.find(named)).toMap()
        val playlists = restorePlaylists(content.playlists, songIds, replace)
        val favourites = restoreFavourites(content.favourites.mapNotNull { songIds[it] }.distinct(), replace, now)
        content.settings?.let { restoreSettings(it, replace) }
        return BackupRestore.Done(favourites, playlists, songsNotFound = songIds.count { it.value == null })
    }

    /**
     * Makes the playlists of the file. Without "replace" a playlist whose name is here already is
     * left as it is, so the same file read twice does not make every playlist twice.
     */
    private suspend fun restorePlaylists(
        saved: List<BackupPlaylist>,
        songIds: Map<String, Long?>,
        replace: Boolean,
    ): Int {
        val before = sources.playlists.playlists()
        val namesHere = before.map { it.name.lowercase() }.toSet()
        val made = saved.filter { replace || it.name.lowercase() !in namesHere }.mapNotNull { playlist ->
            sources.playlists.create(playlist.name)?.also { id ->
                sources.playlists.addAll(id, playlist.songs.mapNotNull { songIds[it] })
            }
        }
        if (replace) before.forEach { sources.playlists.delete(it.id) }
        return made.size
    }

    /**
     * Marks the songs in one step and in the order of the file, so the one marked last is still the
     * first of the list. The times end at [now]: a song marked after the restore is newer than all of them.
     */
    private suspend fun restoreFavourites(songIds: List<Long>, replace: Boolean, now: Long): Int {
        val before = sources.backup.favouriteIds()
        sources.backup.addFavourites(songIds, firstAt = now - songIds.size)
        if (replace) sources.backup.removeFavourites(before - songIds.toSet())
        return songIds.size
    }

    /**
     * Only a folder the library knows is hidden, as in Settings, where the person picks from those.
     * A file can so not hide a whole storage, or fill the list with paths that nobody can find again.
     */
    private suspend fun restoreSettings(saved: BackupSettings, replace: Boolean) {
        val before = sources.settings.scanSettings().first()
        val known = sources.settings.audioFolders().toSet()
        saved.minTrackSeconds?.let { sources.settings.setMinTrackSeconds(it) }
        saved.songSort?.let { sources.settings.setSongSort(it) }
        saved.hiddenFolders.filter { it in known && it !in before.hiddenFolders }
            .forEach { sources.settings.setFolderHidden(it, true) }
        if (replace) {
            (before.hiddenFolders - saved.hiddenFolders.toSet()).forEach { sources.settings.setFolderHidden(it, false) }
        }
    }
}
