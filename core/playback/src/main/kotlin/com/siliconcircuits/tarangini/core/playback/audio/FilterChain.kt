/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow

/**
 * What the equalizer is asked to do: a gain for each band and a bass boost. Flat leaves the sound
 * as it is.
 */
internal data class EqualizerTarget(
    val bandsHz: List<Float> = emptyList(),
    val gainsDb: List<Float> = emptyList(),
    val bassDb: Float = 0f,
) {
    companion object {
        val FLAT = EqualizerTarget()
    }
}

/**
 * The filters of one [EqualizerTarget] for one audio format, with the state of each filter for
 * each channel. The chain always has one filter per band and one for the bass, so the state can
 * pass from one chain to the next.
 *
 * The sound is turned down first by the most the filters lift any tone, so a song at full level
 * does not clip at full boost.
 */
internal class FilterChain private constructor(
    private val filters: Array<Biquad>,
    private val preGain: Double,
    channels: Int,
) {
    private val first = Array(channels) { DoubleArray(filters.size) }
    private val second = Array(channels) { DoubleArray(filters.size) }

    /** True when the chain leaves the sound as it is. */
    val isIdentity: Boolean = preGain == 1.0 && filters.all(Biquad::isIdentity)

    /** One sample of [channel] through the chain, as a value from -1 to 1 and more. */
    fun process(channel: Int, sample: Double): Double {
        val z1 = first[channel]
        val z2 = second[channel]
        var value = sample * preGain
        for (index in filters.indices) {
            val filter = filters[index]
            // The transposed direct form II: two numbers of state per filter.
            val out = filter.b0 * value + z1[index]
            z1[index] = filter.b1 * value - filter.a1 * out + z2[index]
            z2[index] = filter.b2 * value - filter.a2 * out
            value = out
        }
        return value
    }

    /** Takes the state of [other], so a change of the target goes on from the sound that plays. */
    fun takeStateFrom(other: FilterChain) {
        if (other.first.size != first.size || other.filters.size != filters.size) return
        for (channel in first.indices) {
            other.first[channel].copyInto(first[channel])
            other.second[channel].copyInto(second[channel])
        }
    }

    companion object {
        /** How wide each bell is. With bands two octaves apart, 0.9 lets two bells meet without a gap. */
        private const val BAND_Q = 0.9

        /** Below this the bass boost lifts the sound. */
        private const val BASS_HZ = 100.0

        /** A band this close to half of the sample rate is left out: the filter cannot reach it. */
        private const val HIGHEST_SHARE = 0.45

        /** The tones at which the lift of the whole chain is measured to find how far to turn it down. */
        private const val MEASURED_TONES = 128
        private const val LOWEST_TONE_HZ = 20.0
        private const val HIGHEST_TONE_HZ = 20_000.0

        /** dB to an amplitude ratio. */
        private const val AMPLITUDE_DB = 20.0

        /** The chain of [target] for audio at [sampleRate] with [channels] channels. */
        fun of(target: EqualizerTarget, sampleRate: Int, channels: Int): FilterChain {
            val highest = sampleRate * HIGHEST_SHARE
            val bands = target.bandsHz.mapIndexed { index, hz ->
                val gain = target.gainsDb.getOrElse(index) { 0f }.toDouble()
                if (hz > 0f &&
                    hz < highest
                ) {
                    Biquad.peaking(sampleRate, hz.toDouble(), BAND_Q, gain)
                } else {
                    Biquad.IDENTITY
                }
            }
            val filters = (bands + Biquad.lowShelf(sampleRate, BASS_HZ, target.bassDb.toDouble())).toTypedArray()
            return FilterChain(filters, preGainFor(filters, sampleRate, target.bandsHz), channels)
        }

        /** The gain that turns the loudest tone of the chain down to its first level. */
        private fun preGainFor(filters: Array<Biquad>, sampleRate: Int, bandsHz: List<Float>): Double {
            if (filters.all(Biquad::isIdentity)) return 1.0
            val top = min(HIGHEST_TONE_HZ, sampleRate * HIGHEST_SHARE)
            val step = ln(top / LOWEST_TONE_HZ) / (MEASURED_TONES - 1)
            val bands = bandsHz.map(Float::toDouble).filter { it > 0 }
            val tones = List(MEASURED_TONES) { LOWEST_TONE_HZ * exp(step * it) } + bands
            val lift = tones.filter { it <= top }.maxOf { hz -> filters.sumOf { it.gainDbAt(hz, sampleRate) } }
            return if (lift > 0) 10.0.pow(-lift / AMPLITUDE_DB) else 1.0
        }
    }
}
