/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.siliconcircuits.tarangini.core.domain.premium.CrashReporter
import com.siliconcircuits.tarangini.core.domain.premium.ReportedProblem

/**
 * Tells the [CrashReporter] about a player error that points to a fault. Only the name of the
 * error code goes out, never the message of the error: that can hold the address of the file.
 *
 * Not reported: a file that is gone (the person removed it), a file with less sound than its
 * header states and a live stream that fell behind (both are handled), and a stream that cannot
 * be reached (the phone is offline, or the station is).
 */
internal class ReportPlayerErrors(private val crashes: CrashReporter) : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        if (error.errorCode !in EXPECTED) {
            crashes.report(ReportedProblem.PLAYBACK_FAILED, error.errorCodeName)
        }
    }

    private companion object {
        val EXPECTED = setOf(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        )
    }
}
