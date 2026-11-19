/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.session

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Answers the apps that talk to the media session. It checks the caller and every item a caller asks to play. */
internal class PlaybackSessionCallback(private val callers: CallerPolicy) : MediaLibrarySession.Callback {
    // Media3 marks the "trusted by the system" flag as an API that may still change.
    @OptIn(UnstableApi::class)
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult =
        if (callers.allows(controller.packageName, controller.uid, controller.isTrusted)) {
            super.onConnect(session, controller)
        } else {
            MediaSession.ConnectionResult.reject()
        }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> = try {
        Futures.immediateFuture(mediaItems.map { it.toPlayable() })
    } catch (refused: IllegalArgumentException) {
        Futures.immediateFailedFuture(refused)
    }
}
