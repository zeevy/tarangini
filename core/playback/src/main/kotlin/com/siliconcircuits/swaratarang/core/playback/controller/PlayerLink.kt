/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.siliconcircuits.swaratarang.core.playback.controller

import androidx.media3.common.Player
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one connection of the phone UI to the media session, shared by every screen.
 *
 * The connection is open while a screen of the app is open. It is released when the last screen
 * closes, so that the service can stop when nothing plays. When the service stops or crashes while
 * a screen is open, the connection is made again.
 *
 * Everything here must be called on the main thread.
 */
@Singleton
internal class PlayerLink @Inject constructor(private val connector: SessionConnector) {
    /** The player of the session, or null while it is not reachable. */
    var player: Player? = null
        private set

    /** Called with the new value each time [player] changes. */
    var onPlayerChanged: (Player?) -> Unit = {}

    private var openScreens = 0
    private var connection: SessionConnection? = null
    private var connectionNumber = 0

    /** Commands sent while the player was not reachable. They run in order when it is. */
    private val waiting = mutableListOf<(Player) -> Unit>()

    /** A screen of the app was opened. */
    fun onScreenOpened() {
        openScreens++
        connect()
    }

    /**
     * A screen of the app was closed.
     *
     * @param comesBack true when the system closes the screen only to build it again at once (a rotation)
     */
    fun onScreenClosed(comesBack: Boolean) {
        openScreens = (openScreens - 1).coerceAtLeast(0)
        if (openScreens == 0 && !comesBack) {
            waiting.clear()
            drop()
        }
    }

    /**
     * Runs [command] on the player: now when it is reachable, otherwise as soon as it is.
     * With no screen open and no connection the command is ignored: a connection opened for it
     * would have no screen to release it.
     */
    fun send(command: (Player) -> Unit) {
        val reachable = player
        if (reachable != null) {
            command(reachable)
        } else if (openScreens > 0) {
            waiting += command
            connect()
        }
    }

    private fun connect() {
        if (connection != null) return
        val number = ++connectionNumber
        // A callback of a connection that was dropped in the meantime is ignored.
        connection = connector.connect(
            onConnected = { connected -> if (number == connectionNumber) onConnected(connected) },
            onDisconnected = { if (number == connectionNumber) onDisconnected() },
        )
    }

    private fun onConnected(connected: Player) {
        player = connected
        onPlayerChanged(connected)
        val commands = waiting.toList()
        waiting.clear()
        commands.forEach { command -> command(connected) }
    }

    private fun onDisconnected() {
        val wasConnected = player != null
        drop()
        if (wasConnected && openScreens > 0) {
            connect()
        } else {
            // The connection could not be made. Trying again at once would only fail again;
            // the next command or the next screen tries again.
            waiting.clear()
        }
    }

    private fun drop() {
        connectionNumber++
        connection?.release()
        connection = null
        if (player != null) {
            player = null
            onPlayerChanged(null)
        }
    }
}
