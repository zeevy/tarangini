/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.LibraryRepository
import com.siliconcircuits.tarangini.core.playback.browse.BrowseTree
import com.siliconcircuits.tarangini.core.playback.browse.CarSearch
import com.siliconcircuits.tarangini.core.playback.browse.MediaId
import com.siliconcircuits.tarangini.core.playback.browse.PickedQueue
import com.siliconcircuits.tarangini.core.playback.browse.spokenRequest
import com.siliconcircuits.tarangini.core.playback.mapper.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * What the car reads and plays.
 *
 * @property tree the list the car shows
 * @property search finds what the car searches for
 * @property play turns a tap in the car list and a spoken request into a queue
 */
internal class CarLibrary(val tree: BrowseTree, val search: CarSearch, val play: CarPlay)

/** Why a request of the car could not start anything. The car shows a short text for it. */
internal enum class NothingToPlay { NOTHING_FOUND, NO_SONGS }

/**
 * Turns the two requests the car sends without an address into a queue: a
 * tap on a song of the list, and a spoken "play ...". Everything that is
 * played comes from the library of this app; the request only says what to
 * look for.
 *
 * @param scope runs the reads, on the main thread
 * @param resumption what was playing last, for a spoken "play some music"
 */
// The item list with a start place is API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class CarPlay(
    private val scope: CoroutineScope,
    private val tree: BrowseTree,
    private val search: CarSearch,
    private val library: LibraryRepository,
    private val resumption: suspend () -> MediaItemsWithStartPosition?,
) {
    /**
     * The queue for [item], or null when the item is neither a song of the car list nor a spoken request.
     *
     * @param spokenDone is called when a spoken request is answered: with null when there is a queue to
     *   start, and with the reason when there is nothing to play, so the driver is told and does not
     *   sit in silence
     */
    fun queueFor(
        item: MediaItem,
        spokenDone: (NothingToPlay?) -> Unit,
    ): ListenableFuture<MediaItemsWithStartPosition>? {
        val picked = item.pickedSong()
        // The extras come from another app. Extras that cannot be read make the item no spoken request.
        val spoken = runCatching { item.spokenRequest() }.getOrNull()
        return when {
            picked != null -> answer({}) { tree.queue(picked)?.toItems() }

            spoken == null -> null

            spoken.text.isBlank() -> answer({ found -> spokenDone(NothingToPlay.NO_SONGS.takeUnless { found }) }) {
                resumption()
                    ?: someMusic()
            }

            else -> answer({ found ->
                spokenDone(NothingToPlay.NOTHING_FOUND.takeUnless { found })
            }) { search.queue(spoken)?.toItems() }
        }
    }

    /**
     * "Play some music" with nothing to go on with: songs from the whole library in a new order each
     * time, [Page.MAX_LIMIT] at most.
     */
    private suspend fun someMusic(): MediaItemsWithStartPosition? {
        val all = mutableListOf<Song>()
        var pages = 0
        do {
            val part = library.songs(SongSort.TITLE, Page(Page.MAX_LIMIT, all.size))
            all += part
            pages++
        } while (part.size == Page.MAX_LIMIT && pages < MOST_PAGES)
        return all.shuffled().take(Page.MAX_LIMIT).ifEmpty { null }?.let { PickedQueue(it, 0).toItems() }
    }

    /**
     * The caller always gets an answer, also when the read fails. [done] gets true when there is a
     * queue and false when there is not. It is not called when the service is going down.
     */
    private fun answer(
        done: (Boolean) -> Unit,
        read: suspend () -> MediaItemsWithStartPosition?,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val answer = SettableFuture.create<MediaItemsWithStartPosition>()
        // The read starts in this call, so an answer that needs no wait is there when the call returns.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val queue = runCatching { read() }.getOrNull()
            if (queue == null) {
                answer.setException(IllegalArgumentException("The library has nothing to play for this request"))
            } else {
                answer.set(queue)
            }
            if (isActive) done(queue != null)
        }
        return answer
    }

    private fun PickedQueue.toItems() = MediaItemsWithStartPosition(songs.map { it.toMediaItem() }, index, C.TIME_UNSET)
}

// "Play some music" reads at most this many pages of songs: 10,000 songs.
private const val MOST_PAGES = 20

/**
 * The song a caller picked from the list of the car, or null for any
 * other item. Such an item has only an id: an item that names an address
 * is checked as an address, whatever its id says.
 */
private fun MediaItem.pickedSong(): MediaId.SongIn? = if (localConfiguration == null &&
    requestMetadata.mediaUri == null
) {
    MediaId.parse(mediaId) as? MediaId.SongIn
} else {
    null
}
