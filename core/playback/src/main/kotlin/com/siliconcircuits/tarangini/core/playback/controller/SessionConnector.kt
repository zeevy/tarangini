/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.media3.common.Player

/** Opens connections to the media session of the playback service. */
internal interface SessionConnector {
    /**
     * Starts to connect. Both callbacks run on the main thread, and never before this call returns.
     *
     * @param onConnected called once with the player of the session, when the session accepted the connection
     * @param onDisconnected called once when the connection could not be made, or when it ended later
     *   (the service stopped or crashed)
     * @return the connection, to be released when it is no longer needed
     */
    fun connect(onConnected: (Player) -> Unit, onDisconnected: () -> Unit): SessionConnection
}

/** One connection to the media session. */
internal fun interface SessionConnection {
    /** Ends the connection. The player it gave must not be used after this. */
    fun release()
}
