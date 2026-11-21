/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.data.playlist

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.siliconcircuits.tarangini.core.data.R
import com.siliconcircuits.tarangini.core.data.di.IoDispatcher
import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistFileDao
import com.siliconcircuits.tarangini.core.data.local.dao.PlaylistFileRow
import com.siliconcircuits.tarangini.core.domain.model.MAX_PLAYLIST_NAME
import com.siliconcircuits.tarangini.core.domain.playlist.M3u
import com.siliconcircuits.tarangini.core.domain.playlist.M3uSong
import com.siliconcircuits.tarangini.core.domain.playlist.SongFile
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistFiles
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistImport
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Playlist files through the addresses the file picker of the system gives.
 *
 * Only a `content:` address is opened, so a made up address cannot point at a file of the app
 * itself. A file that is read is not trusted: see [M3u].
 */
internal class DefaultPlaylistFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val files: PlaylistFileDao,
    private val playlists: PlaylistRepository,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : PlaylistFiles {
    override suspend fun exportTo(playlistId: Long, target: String): Boolean = withContext(io) {
        runCatching {
            val songs = files.filesOf(playlistId).map { it.toM3uSong() }
            // A playlist with no file left on the phone would give an empty file that looks saved.
            val stream = pickedFile(target).takeIf { songs.isNotEmpty() }
                ?.let { context.contentResolver.openOutputStream(it, "wt") }
            stream?.use { it.write(M3u.write(songs).toByteArray()) } != null
        }.onFailure(::passOnCancel).getOrDefault(false)
    }

    override suspend fun importFrom(source: String): PlaylistImport = withContext(io) {
        runCatching {
            val file = pickedFile(source)
            val bytes = file?.let { context.contentResolver.openInputStream(it) }?.use(::readLimited)
            when {
                file == null || bytes == null -> PlaylistImport.Failed
                bytes.size > M3u.MAX_BYTES -> PlaylistImport.TooLarge
                else -> playlistOf(M3u.entries(textOf(bytes)), nameOf(file))
            }
        }.onFailure(::passOnCancel).getOrDefault(PlaylistImport.Failed)
    }

    /** A cancelled call must end as cancelled, not as a failure that is told. */
    private fun passOnCancel(problem: Throwable) {
        if (problem is CancellationException) throw problem
    }

    /** The text of a file: UTF-8, or the Western code page many older playlist files are written in. */
    private fun textOf(bytes: ByteArray): String =
        bytes.decodeToString().takeIf { BROKEN_CHARACTER !in it } ?: bytes.toString(Charsets.ISO_8859_1)

    private fun pickedFile(address: String): Uri? = Uri.parse(address).takeIf { it.scheme == "content" }

    /** Reads one byte more than a playlist file may have, and never the rest of a larger file. */
    private fun readLimited(stream: InputStream): ByteArray {
        val bytes = ByteArray(M3u.MAX_BYTES + 1)
        var filled = 0
        while (filled < bytes.size) {
            val read = stream.read(bytes, filled, bytes.size - filled)
            if (read < 0) break
            filled += read
        }
        return bytes.copyOf(filled)
    }

    private suspend fun playlistOf(entries: List<String>, name: String): PlaylistImport {
        // The table compares without regard to case only for the letters a to z. So each name is also
        // asked for as the file writes it: "Été.mp3" is then found when the file writes it the same way.
        val names = entries.flatMap { entry -> listOfNotNull(M3u.fileNameOf(entry), writtenNameOf(entry)) }.distinct()
        val known = names.chunked(NAMES_PER_QUERY)
            .flatMap { part -> files.filesNamed(part) }
            .distinctBy { it.songId }
            .map { SongFile(it.songId, it.folderPath, it.fileName) }
            .groupBy { it.fileName.lowercase() }
        val found = entries.map { entry -> entry to M3u.match(entry, known[M3u.fileNameOf(entry)].orEmpty()) }
        val songIds = found.mapNotNull { it.second }
        val playlistId = if (songIds.isEmpty()) null else playlists.create(name)
        return if (playlistId == null) {
            PlaylistImport.NothingFound
        } else {
            // A playlist with nothing in it must not stay behind when the songs cannot be added.
            runCatching { playlists.addAll(playlistId, songIds) }.onFailure { problem ->
                withContext(NonCancellable) { playlists.delete(playlistId) }
                throw problem
            }
            PlaylistImport.Done(
                playlistId = playlistId,
                name = name,
                matched = songIds.size,
                total = entries.size,
                notFound = found.filter { it.second == null }.map { shown(it.first).take(MAX_SHOWN_LINE) }
                    .take(PlaylistImport.MAX_NOT_FOUND),
            )
        }
    }

    /** The name of the file without its ending, as the name of the playlist. */
    private fun nameOf(file: Uri): String {
        val shown = context.contentResolver.query(file, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { row -> if (row.moveToFirst()) row.getString(0) else null }
        val name = shown(shown.orEmpty().substringBeforeLast('.')).trim().take(MAX_PLAYLIST_NAME)
        return name.ifEmpty { context.getString(R.string.playlist_imported) }
    }

    /** The file name as the line writes it, for a path with either kind of slash. */
    private fun writtenNameOf(entry: String): String = entry.replace('\\', '/').substringAfterLast('/')

    /**
     * A text of a file as it may be shown: no control character, and no character that changes the
     * direction of the text and so could make a line read as something else.
     */
    private fun shown(text: String): String =
        text.filterNot { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }

    private companion object {
        const val BROKEN_CHARACTER = '\uFFFD'
        const val NAMES_PER_QUERY = 400
        const val MAX_SHOWN_LINE = 200
    }
}

/**
 * The path other players on a phone use: the built-in storage is `/storage/emulated/0`, a card
 * is `/storage/<its code>`.
 */
private fun PlaylistFileRow.toM3uSong(): M3uSong {
    val volume = folderPath.substringBefore('/')
    val inside = folderPath.substringAfter('/', "")
    val isBuiltIn = volume == MediaStore.VOLUME_EXTERNAL_PRIMARY
    val root = if (isBuiltIn) "/storage/emulated/0" else "/storage/${volume.uppercase()}"
    val folder = if (inside.isEmpty()) root else "$root/$inside"
    return M3uSong(path = "$folder/$fileName", title = title, artist = artist, durationMs = durationMs)
}
