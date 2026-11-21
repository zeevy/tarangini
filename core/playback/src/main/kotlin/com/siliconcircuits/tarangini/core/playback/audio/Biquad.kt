/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The numbers of one second order filter, already divided by `a0`. The formulas are the ones of
 * the "Audio EQ Cookbook" by Robert Bristow-Johnson, which most equalizers use.
 */
internal data class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
    /** True for a filter that leaves the sound as it is. */
    val isIdentity: Boolean get() = this == IDENTITY

    /** How much the filter lifts or cuts a tone of [hz], in dB. */
    fun gainDbAt(hz: Double, sampleRate: Int): Double {
        val w = 2 * PI * hz / sampleRate
        val realTop = b0 + b1 * cos(w) + b2 * cos(2 * w)
        val imaginaryTop = -(b1 * sin(w) + b2 * sin(2 * w))
        val realBottom = 1 + a1 * cos(w) + a2 * cos(2 * w)
        val imaginaryBottom = -(a1 * sin(w) + a2 * sin(2 * w))
        val power = (realTop * realTop + imaginaryTop * imaginaryTop) /
            (realBottom * realBottom + imaginaryBottom * imaginaryBottom)
        return POWER_DB * log10(power)
    }

    companion object {
        /** The filter that changes nothing. */
        val IDENTITY = Biquad(1.0, 0.0, 0.0, 0.0, 0.0)

        /** A bell around [hz] that lifts or cuts by [gainDb]; [q] sets how wide the bell is. */
        fun peaking(sampleRate: Int, hz: Double, q: Double, gainDb: Double): Biquad {
            if (gainDb == 0.0) return IDENTITY
            val a = amplitude(gainDb)
            val w = 2 * PI * hz / sampleRate
            val alpha = sin(w) / (2 * q)
            val a0 = 1 + alpha / a
            return Biquad(
                b0 = (1 + alpha * a) / a0,
                b1 = -2 * cos(w) / a0,
                b2 = (1 - alpha * a) / a0,
                a1 = -2 * cos(w) / a0,
                a2 = (1 - alpha / a) / a0,
            )
        }

        /** Lifts or cuts everything below [hz] by [gainDb], with the steepest slope that has no bump. */
        fun lowShelf(sampleRate: Int, hz: Double, gainDb: Double): Biquad {
            if (gainDb == 0.0) return IDENTITY
            val a = amplitude(gainDb)
            val w = 2 * PI * hz / sampleRate
            val c = cos(w)
            // A slope of 1: alpha = sin(w) / 2 * sqrt(2).
            val rootAlpha = 2 * sqrt(a) * sin(w) / 2 * sqrt(2.0)
            val a0 = (a + 1) + (a - 1) * c + rootAlpha
            return Biquad(
                b0 = a * ((a + 1) - (a - 1) * c + rootAlpha) / a0,
                b1 = 2 * a * ((a - 1) - (a + 1) * c) / a0,
                b2 = a * ((a + 1) - (a - 1) * c - rootAlpha) / a0,
                a1 = -2 * ((a - 1) + (a + 1) * c) / a0,
                a2 = ((a + 1) + (a - 1) * c - rootAlpha) / a0,
            )
        }

        private fun amplitude(gainDb: Double) = 10.0.pow(gainDb / SHELF_AND_PEAK_DB)

        /** dB of a power ratio. */
        private const val POWER_DB = 10.0

        /** The cookbook takes the square root of the amplitude: 10 to the power of gain / 40. */
        private const val SHELF_AND_PEAK_DB = 40.0
    }
}
