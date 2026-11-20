/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.siliconcircuits.tarangini.core.domain.repository.FavouriteRepository
import com.siliconcircuits.tarangini.core.domain.repository.SongLookup
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The heart button next to the player buttons of the notification, the lock screen and the car.
 * It shows whether the song that plays is a favourite, and a press changes that. The press names
 * no song: it always means the song that plays, so a caller cannot mark anything else.
 *
 * @param label the name of the button: what a press does when the song is a favourite (true) or is not
 */
// The buttons of a session are API of Media3 that may still change.
@OptIn(UnstableApi::class)
internal class FavouriteButton(
    private val scope: CoroutineScope,
    private val favourites: FavouriteRepository,
    private val songs: SongLookup,
    private val label: (isFavourite: Boolean) -> String,
) {
    private val playing = MutableStateFlow<String?>(null)
    private var mark: Mark? = null

    /** Tells which item plays now, by its id in the queue. Null when nothing is loaded. */
    fun nowPlaying(mediaId: String?) {
        // Until the mark of the new item is read, a press does nothing. It must never reach the item before.
        if (mediaId != playing.value) mark = null
        playing.value = mediaId
    }

    /**
     * Hands [show] the buttons to show, now and after every change of the song or of its mark.
     * An item that is no song of the library gets no button.
     */
    fun start(show: (List<CommandButton>) -> Unit): Job = scope.launch {
        val song = playing.map { id -> id?.let { songIdOf(it) } }
        combine(song, favourites.songIds()) { songId, ids -> songId?.let { Mark(it, it in ids) } }
            .distinctUntilChanged()
            .collect { now ->
                mark = now
                show(listOfNotNull(now?.let(::button)))
            }
    }

    /** A press of the button. A mark that cannot be saved leaves the button as it was. */
    fun toggle() {
        val songId = mark?.songId ?: return
        // The mark is read again here, so two quick presses are two changes.
        scope.launch { runCatching { favourites.setFavourite(songId, songId !in favourites.songIds().first()) } }
    }

    private fun button(mark: Mark): CommandButton =
        CommandButton.Builder(if (mark.on) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
            .setDisplayName(label(mark.on))
            .setSessionCommand(COMMAND)
            .build()

    private suspend fun songIdOf(mediaId: String): Long? =
        runCatching { songs.byMediaUri(listOf(mediaId))[mediaId]?.id }
            .onFailure { if (it is CancellationException) throw it }
            .getOrNull()

    private data class Mark(val songId: Long, val on: Boolean)

    companion object {
        /** The command the button sends. It carries no value. */
        val COMMAND = SessionCommand("com.siliconcircuits.tarangini.TOGGLE_FAVOURITE", Bundle.EMPTY)
    }
}
