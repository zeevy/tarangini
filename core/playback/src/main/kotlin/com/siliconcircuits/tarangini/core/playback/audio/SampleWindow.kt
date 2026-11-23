/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

/**
 * The newest [SIZE] samples of the sound, left and right mixed, with their sample rate. The
 * playback thread writes, the visual reads a copy; both hold the lock only for a short copy.
 */
internal class SampleWindow {
    private val ring = FloatArray(SIZE)
    private var next = 0
    private val lock = Any()

    /** The sample rate of the newest samples in Hz, or 0 before any came. */
    @Volatile
    var sampleRate: Int = 0
        private set

    /** Adds [count] samples from [samples], oldest first, at [rate] Hz. Called on the playback thread. */
    fun add(samples: FloatArray, count: Int, rate: Int) {
        synchronized(lock) {
            sampleRate = rate
            for (i in 0 until count) {
                ring[next] = samples[i]
                next = (next + 1) % SIZE
            }
        }
    }

    /** A copy of the window, oldest sample first. */
    fun copy(): FloatArray = synchronized(lock) { FloatArray(SIZE) { ring[(next + it) % SIZE] } }

    /** Forgets every sample, so an old song does not show when the next one starts. */
    fun clear() {
        synchronized(lock) {
            ring.fill(0f)
            next = 0
        }
    }

    companion object {
        /** Enough for a spectrum with 23 Hz steps at 48 kHz, and little to copy 25 times a second. */
        const val SIZE = 2048
    }
}
