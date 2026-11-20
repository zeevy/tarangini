/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.session

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Answers the apps that talk to the media session. It checks the caller and every item a caller asks to play.
 *
 * @param scope runs the read of the saved queue and of the texts, on the main thread
 * @param texts gives the items of the queue their title, artist and album
 * @param resumption gives what to play when a caller asks to go on and the player is empty, or null for nothing
 */
// Media3 marks parts of the session API as API that may still change: the "trusted by the system" flag,
// the default command sets and everything about playback resumption.
@OptIn(UnstableApi::class)
internal class PlaybackSessionCallback(
    private val callers: CallerPolicy,
    private val scope: CoroutineScope,
    private val texts: suspend (List<MediaItem>) -> List<MediaItem>,
    private val resumption: suspend () -> MediaItemsWithStartPosition?,
) : MediaLibrarySession.Callback {
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult =
        when (callers.accessFor(controller.packageName, controller.uid, controller.isTrusted)) {
            CallerAccess.FULL -> MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
            )

            CallerAccess.TRANSPORT_ONLY -> MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .removeAll(Player.COMMAND_SET_MEDIA_ITEM, Player.COMMAND_CHANGE_MEDIA_ITEMS)
                    .build(),
            )

            CallerAccess.REFUSED -> MediaSession.ConnectionResult.reject()
        }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> {
        val playable = try {
            mediaItems.map { it.toPlayable() }
        } catch (refused: IllegalArgumentException) {
            return Futures.immediateFailedFuture(refused)
        }
        val answer = SettableFuture.create<List<MediaItem>>()
        // The read starts in this call, so an answer that needs no wait is there when the call returns.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                answer.set(texts(playable))
            } finally {
                // The caller always gets an answer, also when the read is given up. A second set does nothing.
                answer.set(playable)
            }
        }
        return answer
    }

    /**
     * Called when a play request comes and the player is empty, for example
     * the play button of a Bluetooth device after the app was killed. The
     * items come from the database of this app and never from the caller.
     */
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean,
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val answer = SettableFuture.create<MediaItemsWithStartPosition>()
        scope.launch {
            // The caller always gets an answer, also when the read fails.
            val saved = runCatching { resumption() }.getOrNull()
            if (saved == null) {
                answer.setException(UnsupportedOperationException("Nothing was playing before"))
            } else {
                answer.set(saved)
            }
        }
        return answer
    }
}
