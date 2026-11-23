/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.audio

import kotlinx.coroutines.flow.Flow

/** The level of the sound that plays, for a level meter on the screen. */
fun interface AudioLevels {
    /**
     * The level of the left and the right channel, read from the sound the player plays after
     * the equalizer and volume levelling, before the volume of the player. A new reading comes about 25 times a second while the
     * level moves; when nothing plays the level falls to [LevelReading.SILENCE]. The sound is
     * measured only while this flow is collected.
     */
    fun levels(): Flow<LevelReading>
}

/**
 * The peak level of each channel in dB below full scale: 0 is the loudest a sample can be,
 * [SILENCE_DB] is silence. A mono song has the same level on both sides.
 */
data class LevelReading(val leftDb: Float, val rightDb: Float) {
    companion object {
        /** No sound on either side. */
        val SILENCE = LevelReading(SILENCE_DB, SILENCE_DB)
    }
}

/** The lowest level a meter shows. Anything quieter counts as silence. */
const val SILENCE_DB = -60f
