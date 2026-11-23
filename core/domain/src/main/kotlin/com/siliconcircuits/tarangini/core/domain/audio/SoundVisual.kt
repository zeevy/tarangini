/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.audio

import kotlinx.coroutines.flow.Flow

/** The picture of the sound that plays, for the visual on the player: a spectrum and a short wave. */
fun interface SoundVisual {
    /**
     * A new frame about 25 times a second while the sound changes, read from the sound the player
     * plays after volume levelling and before the volume of the player. When nothing plays the
     * bars fall to zero and the wave lies flat. The sound is read only while this flow is collected.
     */
    fun frames(): Flow<VisualFrame>
}

/**
 * One picture of the sound.
 *
 * @property bands the loudness of [BAND_COUNT] bands from low to high notes, each from 0 (silence)
 *   to 1 (full scale), on a scale in dB so a quiet band still shows
 * @property wave [WAVE_POINTS] samples of the newest sound, from -1 to 1, left and right mixed
 */
data class VisualFrame(val bands: List<Float>, val wave: List<Float>) {
    companion object {
        /** The bars of the visual on the player. */
        const val BAND_COUNT = 32

        /** The points of the wave line. */
        const val WAVE_POINTS = 96

        /** No sound: every bar at zero and a flat wave. */
        val SILENT = VisualFrame(List(BAND_COUNT) { 0f }, List(WAVE_POINTS) { 0f })
    }
}
