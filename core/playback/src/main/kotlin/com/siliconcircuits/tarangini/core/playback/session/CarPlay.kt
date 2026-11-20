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
 * @param tell is called when a spoken request finds nothing, so the driver is told and does not sit in silence
 */
// The item list with a start place is API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class CarPlay(
    private val scope: CoroutineScope,
    private val tree: BrowseTree,
    private val search: CarSearch,
    private val library: LibraryRepository,
    private val resumption: suspend () -> MediaItemsWithStartPosition?,
    private val tell: (NothingToPlay) -> Unit,
) {
    /** The queue for [item], or null when the item is neither a song of the car list nor a spoken request. */
    fun queueFor(item: MediaItem): ListenableFuture<MediaItemsWithStartPosition>? {
        val picked = item.pickedSong()
        val spoken = item.spokenRequest()
        return when {
            picked != null -> answer(missing = null) { tree.queue(picked)?.toItems() }
            spoken == null -> null
            spoken.words.isEmpty() -> answer(NothingToPlay.NO_SONGS) { resumption() ?: someMusic() }
            else -> answer(NothingToPlay.NOTHING_FOUND) { search.queue(spoken)?.toItems() }
        }
    }

    /** "Play some music" with nothing to go on with: songs of the library in a new order each time. */
    private suspend fun someMusic(): MediaItemsWithStartPosition? =
        library.songs(SongSort.TITLE, Page(Page.MAX_LIMIT)).shuffled().ifEmpty {
            null
        }?.let { PickedQueue(it, 0).toItems() }

    /** The caller always gets an answer, also when the read fails. [missing] is told when there is nothing to play. */
    private fun answer(
        missing: NothingToPlay?,
        read: suspend () -> MediaItemsWithStartPosition?,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val answer = SettableFuture.create<MediaItemsWithStartPosition>()
        // The read starts in this call, so an answer that needs no wait is there when the call returns.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val queue = runCatching { read() }.getOrNull()
            if (queue == null) {
                answer.setException(IllegalArgumentException("The library has nothing to play for this request"))
                if (missing != null) tell(missing)
            } else {
                answer.set(queue)
            }
        }
        return answer
    }

    private fun PickedQueue.toItems() = MediaItemsWithStartPosition(songs.map { it.toMediaItem() }, index, C.TIME_UNSET)
}

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
