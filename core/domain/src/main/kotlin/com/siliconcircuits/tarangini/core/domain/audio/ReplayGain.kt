/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.audio

import kotlin.math.log10
import kotlin.math.min

/** How the songs are brought to a similar loudness. */
enum class VolumeLevelling {
    /** Every song plays as its file is. */
    OFF,

    /** Each song by its own gain, so songs from different albums sound alike in a mix. */
    TRACK,

    /** Each song by the gain of its album, so the steps in loudness within an album stay. */
    ALBUM,
}

/**
 * The ReplayGain tags of one file. A gain is in dB; a peak is the loudest sample as a share of
 * full scale (1 is full scale). Each value is null when the file has no such tag.
 */
data class ReplayGain(
    val trackGainDb: Float? = null,
    val trackPeak: Float? = null,
    val albumGainDb: Float? = null,
    val albumPeak: Float? = null,
) {
    /** True when the file has no gain at all to go by. */
    val isEmpty: Boolean get() = trackGainDb == null && albumGainDb == null

    /**
     * The gain in dB a song plays with for [levelling]. With no tag of the asked kind the other
     * kind is used. A lift never makes the loudest sample pass full scale: it stops at the peak,
     * and with no peak the song is only turned down, never up.
     */
    fun levelDb(levelling: VolumeLevelling): Float {
        val track = trackGainDb?.let { it to trackPeak }
        val album = albumGainDb?.let { it to albumPeak }
        val picked = when (levelling) {
            VolumeLevelling.OFF -> null
            VolumeLevelling.TRACK -> track ?: album
            VolumeLevelling.ALBUM -> album ?: track
        } ?: return 0f
        val gain = picked.first.coerceIn(-MAX_CUT_DB, MAX_LIFT_DB)
        return if (gain > 0f) min(gain, headroomDb(picked.second)) else gain
    }

    companion object {
        /** A gain outside these limits is a broken tag; it is held at the limit. */
        private const val MAX_CUT_DB = 24f
        private const val MAX_LIFT_DB = 12f

        /** dB to an amplitude ratio. */
        private const val AMPLITUDE_DB = 20f

        /** How far a song with [peak] can be lifted before it clips. */
        private fun headroomDb(peak: Float?): Float =
            if (peak == null) 0f else (-AMPLITUDE_DB * log10(peak)).coerceAtLeast(0f)

        /**
         * The gain in a tag text such as "-6.54 dB" or "+2.10 dB", or null when the text holds no
         * number.
         */
        fun gainOf(text: String?): Float? = numberAtStart(text)

        /** The peak in a tag text such as "0.988525"; null for no number, 0 or less. */
        fun peakOf(text: String?): Float? = numberAtStart(text)?.takeIf { it > 0f }

        private fun numberAtStart(text: String?): Float? {
            // A tool on a system that writes a decimal comma gives "-6,5 dB".
            val trimmed = text?.trim()?.take(MAX_TEXT).orEmpty().replace(',', '.')
            val number = NUMBER.find(trimmed)?.value ?: return null
            return number.toFloatOrNull()?.takeIf(Float::isFinite)
        }

        /** A tag value is short; anything longer is cut before it is read. */
        private const val MAX_TEXT = 32
        private val NUMBER = Regex("""^[+-]?(\d+(\.\d*)?|\.\d+)""")
    }
}
