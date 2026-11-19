/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
