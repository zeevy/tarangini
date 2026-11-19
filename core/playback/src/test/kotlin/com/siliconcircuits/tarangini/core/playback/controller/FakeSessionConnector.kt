/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.controller

import androidx.media3.common.Player

/**
 * A session connector for tests. Nothing connects by itself: the test says when the session
 * accepts ([accept]), refuses ([refuse]) or goes away ([serviceStops]).
 */
internal class FakeSessionConnector : SessionConnector {
    private class Attempt(val onConnected: (Player) -> Unit, val onDisconnected: () -> Unit) {
        var released = false
    }

    private val attempts = mutableListOf<Attempt>()

    /** How many times a connection was asked for. */
    val attemptCount: Int get() = attempts.size

    /** True when the newest connection was released. */
    val lastReleased: Boolean get() = attempts.last().released

    override fun connect(onConnected: (Player) -> Unit, onDisconnected: () -> Unit): SessionConnection {
        val attempt = Attempt(onConnected, onDisconnected)
        attempts += attempt
        return SessionConnection { attempt.released = true }
    }

    /** The session accepts the newest connection and hands out [player]. */
    fun accept(player: Player) = attempts.last().onConnected(player)

    /** The session refuses the newest connection. */
    fun refuse() = attempts.last().onDisconnected()

    /** The service behind the newest connection stops or crashes. */
    fun serviceStops() = attempts.last().onDisconnected()

    /** A late callback of an older connection arrives. */
    fun lateDisconnectOf(attemptIndex: Int) = attempts[attemptIndex].onDisconnected()
}
