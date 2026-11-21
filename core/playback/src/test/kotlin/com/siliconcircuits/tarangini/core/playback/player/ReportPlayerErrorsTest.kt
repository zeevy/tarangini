/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import androidx.media3.common.PlaybackException
import com.siliconcircuits.tarangini.core.domain.premium.ReportedProblem
import com.siliconcircuits.tarangini.core.testing.premium.FakeCrashReporter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportPlayerErrorsTest {
    private val crashes = FakeCrashReporter()
    private val listener = ReportPlayerErrors(crashes)

    private fun failWith(code: Int) =
        listener.onPlayerError(PlaybackException("content://media/external/audio/media/7 failed", null, code))

    @Test
    fun `a player error is reported by the name of its code, without its message`() {
        failWith(PlaybackException.ERROR_CODE_DECODING_FAILED)

        assertEquals(listOf(ReportedProblem.PLAYBACK_FAILED to "ERROR_CODE_DECODING_FAILED"), crashes.reports)
    }

    @Test
    fun `an error that is handled, or only says the phone is offline, is not reported`() {
        failWith(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        failWith(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        failWith(PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW)
        failWith(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        failWith(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)

        assertEquals(emptyList<Pair<ReportedProblem, String>>(), crashes.reports)
    }
}
