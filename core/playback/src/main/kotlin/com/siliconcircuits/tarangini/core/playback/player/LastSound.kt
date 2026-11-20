/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.player

import android.os.SystemClock

/**
 * When the decoder last gave sound to the audio output.
 *
 * The audio renderer writes it on the playback thread, and [SkipSilentEnd] reads it on the main
 * thread to tell a file that still gives sound from one that has none left.
 */
internal class LastSound(private val now: () -> Long = SystemClock::elapsedRealtime) {
    @Volatile
    private var heardAt = now()

    /** Sound was handed to the audio output just now. Also called to start counting afresh. */
    fun heard() {
        heardAt = now()
    }

    /** The time since the last sound, in milliseconds. */
    fun silentForMs(): Long = now() - heardAt
}
