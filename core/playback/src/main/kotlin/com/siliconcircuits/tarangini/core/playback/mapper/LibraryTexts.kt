/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.mapper

import androidx.media3.common.MediaItem
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.yield

/**
 * Gives the items of the queue their title, artist and album from the
 * library. The player reads these from the file only for the item it
 * plays, so without this every other item of the queue has no texts, and
 * so has the current item of a queue that came back at the start.
 *
 * The texts come from the database of this app, never from the caller that
 * sent the item.
 */
internal class LibraryTexts(private val songs: SongLookup) {
    /**
     * The same items in the same order, each with the texts of its song. An item the library does
     * not know stays as it is. So do all of them when the read fails: a song must play without its texts.
     */
    suspend fun addTo(items: List<MediaItem>): List<MediaItem> {
        val found = runCatching { songs.byMediaUri(items.mapNotNull { it.address() }) }
            .getOrElse { failure -> if (failure is CancellationException) throw failure else emptyMap() }
        return items.chunked(PART).flatMap { part ->
            // A queue can have many thousand items. The main thread draws and takes input between two parts.
            yield()
            part.map { item ->
                val song = found[item.address()] ?: return@map item
                // The id and the address stay the checked ones of the item. Only the texts are added.
                item.buildUpon().setMediaMetadata(song.toMediaItem().mediaMetadata).build()
            }
        }
    }

    private fun MediaItem.address(): String? = localConfiguration?.uri?.toString()

    private companion object {
        const val PART = 500
    }
}
