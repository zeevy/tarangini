/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.backup

import android.content.Context
import android.net.Uri
import com.siliconcircuits.tarangini.core.data.di.IoDispatcher
import com.siliconcircuits.tarangini.core.data.mediastore.TimeSource
import com.siliconcircuits.tarangini.core.domain.repository.BackupFiles
import com.siliconcircuits.tarangini.core.domain.repository.BackupRestore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Backups through the addresses the file picker of the system gives.
 *
 * Only a `content:` address is opened, so a made up address cannot point at a file of the app
 * itself. A file that is read is not trusted: see [BackupFormat].
 */
internal class DefaultBackupFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sources: BackupSources,
    private val time: TimeSource,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : BackupFiles {
    private val restorer = BackupRestorer(sources)

    override suspend fun backUpTo(target: String): Boolean = withContext(io) {
        runCatching {
            val text = BackupFormat.write(content(), time.nowMs())
            pickedFile(target)?.let { context.contentResolver.openOutputStream(it, "wt") }
                ?.use { it.write(text.toByteArray()) } != null
        }.onFailure(::passOnCancel).getOrDefault(false)
    }

    override suspend fun restoreFrom(source: String, replace: Boolean): BackupRestore = withContext(io) {
        // A file with brackets nested very deep can end the read with an error that is no exception
        // of the reader. It is caught here like every other failure, and nothing was changed by then.
        runCatching {
            val bytes = pickedFile(source)?.let { context.contentResolver.openInputStream(it) }?.use(::readLimited)
            when {
                bytes == null -> BackupRestore.Failed
                bytes.size > BackupFormat.MAX_BYTES -> BackupRestore.TooLarge
                else -> restore(BackupFormat.read(bytes.decodeToString()), replace)
            }
        }.onFailure(::passOnCancel).getOrDefault(BackupRestore.Failed)
    }

    private suspend fun restore(read: BackupRead, replace: Boolean): BackupRestore = when (read) {
        BackupRead.NotABackup -> BackupRestore.NotABackup

        BackupRead.Newer -> BackupRestore.NewerVersion

        is BackupRead.Read -> {
            val namesSongs =
                read.content.favourites.isNotEmpty() || read.content.playlists.any { it.songs.isNotEmpty() }
            // Before the first scan no song can be found, and every favourite of the file would be lost.
            if (namesSongs && !sources.backup.hasSongs()) {
                BackupRestore.NoLibrary
            } else {
                restorer.restore(read.content, replace, time.nowMs())
            }
        }
    }

    private suspend fun content(): BackupContent {
        val scan = sources.settings.scanSettings().first()
        return BackupContent(
            settings = BackupSettings(
                minTrackSeconds = scan.minTrackSeconds,
                hiddenFolders = scan.hiddenFolders.sorted(),
                songSort = sources.settings.songSort().first(),
            ),
            favourites = sources.backup.favouriteFiles().map { "${it.folderPath}/${it.fileName}" },
            playlists = sources.playlists.playlists().map { playlist ->
                BackupPlaylist(
                    name = playlist.name,
                    songs = sources.backup.playlistFiles(playlist.id).map { "${it.folderPath}/${it.fileName}" },
                )
            },
        )
    }

    /** A cancelled call must end as cancelled, not as a failure that is told. */
    private fun passOnCancel(problem: Throwable) {
        if (problem is CancellationException) throw problem
    }

    private fun pickedFile(address: String): Uri? = Uri.parse(address).takeIf { it.scheme == "content" }

    /** Reads one byte more than a backup may have, and never the rest of a larger file. */
    private fun readLimited(stream: InputStream): ByteArray {
        val bytes = ByteArray(BackupFormat.MAX_BYTES + 1)
        var filled = 0
        var read = 0
        while (read >= 0 && filled < bytes.size) {
            filled += read
            read = stream.read(bytes, filled, bytes.size - filled)
        }
        return bytes.copyOf(filled)
    }
}
