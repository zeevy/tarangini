/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.siliconcircuits.swaratarang.core.playback.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import javax.inject.Inject

/**
 * Connects to the playback service with a Media3 `MediaController`.
 *
 * The controller is built with the application context: one built with the context of a screen
 * crashes the app when the service stops after that screen is gone.
 */
internal class MediaControllerConnector @Inject constructor(@param:ApplicationContext private val context: Context) :
    SessionConnector {
    override fun connect(onConnected: (Player) -> Unit, onDisconnected: () -> Unit): SessionConnection {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val listener = object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) = onDisconnected()
        }
        val future = MediaController.Builder(context, token).setListener(listener).buildAsync()
        future.addListener(
            {
                val controller = try {
                    future.get()
                } catch (_: ExecutionException) {
                    // The session refused the connection or the service could not start.
                    null
                } catch (_: CancellationException) {
                    // Released before the connection was made. Nobody waits for it any more.
                    return@addListener
                }
                if (controller == null) onDisconnected() else onConnected(controller)
            },
            context.mainExecutor,
        )
        return SessionConnection { MediaController.releaseFuture(future) }
    }
}
