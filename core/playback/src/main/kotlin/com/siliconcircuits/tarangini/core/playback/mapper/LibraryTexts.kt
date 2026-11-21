/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.mapper

import android.net.Uri
import androidx.media3.common.MediaItem
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.repository.GenreRepository
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import com.siliconcircuits.tarangini.core.domain.repository.StationStreams
import com.siliconcircuits.tarangini.core.playback.stream.isStreamAddress
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.yield

/**
 * Gives the items of the queue their title, artist and album from the
 * library. The player reads these from the file only for the item it
 * plays, so without this every other item of the queue has no texts, and
 * so has the current item of a queue that came back at the start.
 *
 * The texts come from the database of this app, never from the caller that
 * sent the item. So do the genres of the song, which pick the equalizer
 * preset of its genre.
 */
internal class LibraryTexts(
    private val songs: SongLookup,
    private val stations: StationStreams,
    private val genres: GenreRepository,
) {
    /**
     * The same items in the same order, each with the texts of its song. An item the library does
     * not know stays as it is. So do all of them when the read fails: a song must play without its texts.
     */
    suspend fun addTo(items: List<MediaItem>): List<MediaItem> {
        val addresses = items.mapNotNull { it.address() }
        val found = read(emptyMap()) { songs.byMediaUri(addresses) }
        val genreKeys = if (found.isEmpty()) {
            emptyMap()
        } else {
            read(emptyMap()) {
                genres.genreKeysOf(found.values.map(Song::id))
            }
        }
        val streams = addresses.filter { Uri.parse(it).isStreamAddress() }
        // A stream gets the name its station was stored with. The server is asked for a name only when there is none.
        val names = if (streams.isEmpty()) emptyMap() else read(emptyMap()) { stations.namesByAddress(streams) }
        return items.chunked(PART).flatMap { part ->
            // A queue can have many thousand items. The main thread draws and takes input between two parts.
            yield()
            part.map { item ->
                val song = found[item.address()]
                item.withTexts(song, names[item.address()], song?.let { genreKeys[it.id] }.orEmpty())
            }
        }
    }

    /** The id and the address stay the checked ones of the item. Only the texts are added. */
    private fun MediaItem.withTexts(song: Song?, station: String?, genreKeys: List<String>): MediaItem = when {
        song != null -> buildUpon().setMediaMetadata(song.toMediaItem().mediaMetadata.withGenreKeys(genreKeys)).build()

        station != null ->
            buildUpon().setMediaMetadata(
                mediaMetadata.buildUpon().setStation(station).setTitle(station).build(),
            ).build()

        else -> this
    }

    /** A failed read leaves the items without texts. It must not stop them from playing. */
    private suspend fun <T> read(fallback: T, block: suspend () -> T): T = runCatching { block() }
        .getOrElse { failure -> if (failure is CancellationException) throw failure else fallback }

    private fun MediaItem.address(): String? = localConfiguration?.uri?.toString()

    private companion object {
        const val PART = 500
    }
}
