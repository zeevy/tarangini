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
 * Tells the [CrashReporter] when the player gives up on an item. Only the name of the error code
 * goes out, never the message of the error: that can hold the address of the file.
 *
 * A file that is gone is not reported. The person removed it, and the player skips it.
 */
internal class ReportPlayerErrors(private val crashes: CrashReporter) : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        if (error.errorCode != PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) {
            crashes.report(ReportedProblem.PLAYBACK_FAILED, error.errorCodeName)
        }
    }
}
