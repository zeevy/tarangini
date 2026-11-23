/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.library.list

import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.model.QueueSource
import com.siliconcircuits.tarangini.core.domain.model.Song
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackController
import com.siliconcircuits.tarangini.core.domain.playback.PlaybackQueue
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.PlaylistRepository
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.component.pickIn
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What a tap on a song or on its menu does. Every list of songs uses it, so they all behave the same. */
internal class SongActions @Inject constructor(
    private val playback: PlaybackController,
    private val queue: PlaybackQueue,
    private val favourites: FavouriteRepository,
    private val playlists: PlaylistRepository,
) {
    /**
     * The actions for the rows of one list. [read] gives that list page by
     * page, in the order the screen shows it. A tap plays the song with the
     * whole list as the queue, and a second tap takes the place of a first
     * one that still reads the list. [source] names the list for now playing.
     */
    fun rowActions(
        scope: CoroutineScope,
        source: QueueSource? = null,
        read: suspend (Page) -> List<Song>,
    ): SongRowActions {
        var starting: Job? = null
        return SongRowActions(
            onPlay = { song ->
                starting?.cancel()
                starting = scope.launch { play(song, source, read) }
            },
            onPlayNext = { queue.playNext(listOf(it.mediaUri)) },
            onAddToQueue = { queue.addToEnd(listOf(it.mediaUri)) },
            favourites = favourites.songIds().stateIn(scope, SharingStarted.Eagerly, emptySet()),
            onSetFavourite = { song, favourite ->
                scope.launch { attempt { favourites.setFavourite(song.id, favourite) } }
            },
            playlists = playlists.pickIn(scope),
        )
    }

    /**
     * The place of the song is looked up in what is read now, because the
     * list may have changed since the screen read it. A song that is not in
     * the list any more, or a list that cannot be read, plays the song alone.
     */
    private suspend fun play(song: Song, source: QueueSource?, read: suspend (Page) -> List<Song>) {
        val uris = attempt { wholeList(read) }.getOrDefault(emptyList())
        val index = uris.indexOf(song.mediaUri)
        // A song played alone is not the list any more, so it has no source.
        if (index >= 0) playback.play(uris, index, source) else playback.play(listOf(song.mediaUri))
    }

    private suspend fun wholeList(read: suspend (Page) -> List<Song>): List<String> {
        val uris = mutableListOf<String>()
        do {
            val page = read(Page(Page.MAX_LIMIT, uris.size))
            uris += page.map { it.mediaUri }
        } while (page.size == Page.MAX_LIMIT)
        return uris
    }
}
