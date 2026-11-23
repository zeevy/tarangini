/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The loudness of [VisualFrame.BAND_COUNT] bands of [samples], from 0 to 1. The bands are spread
 * evenly over the notes (on a log scale) from [LOWEST_HZ] to [HIGHEST_HZ], the way an ear hears them;
 * each band is the strongest frequency inside it. [samples] must have a length that is a power of 2.
 */
internal fun spectrumOf(samples: FloatArray, sampleRate: Int): List<Float> {
    if (sampleRate <= 0) return VisualFrame.SILENT.bands
    val size = samples.size
    val real = FloatArray(size) { samples[it] * hann(it, size) }
    val imaginary = FloatArray(size)
    fft(real, imaginary)
    val binHz = sampleRate.toFloat() / size
    val top = minOf(HIGHEST_HZ, sampleRate / 2f)
    val ratio = (top / LOWEST_HZ).pow(1f / VisualFrame.BAND_COUNT)
    return List(VisualFrame.BAND_COUNT) { band ->
        val from = LOWEST_HZ * ratio.pow(band)
        val to = from * ratio
        val first = (from / binHz).toInt().coerceIn(1, size / 2 - 1)
        val last = (to / binHz).toInt().coerceIn(first, size / 2 - 1)
        var strongest = 0f
        for (bin in first..last) {
            strongest = maxOf(strongest, sqrt(real[bin] * real[bin] + imaginary[bin] * imaginary[bin]))
        }
        // A full scale sine gives about size / 4 after the Hann window.
        loudness(strongest / (size / FULL_SCALE_DIVIDER))
    }
}

/** [VisualFrame.WAVE_POINTS] samples from the end of [samples], evenly apart, cut to -1..1. */
internal fun waveOf(samples: FloatArray): List<Float> {
    val step = WAVE_SPAN / VisualFrame.WAVE_POINTS
    val start = samples.size - WAVE_SPAN
    return List(VisualFrame.WAVE_POINTS) { samples[start + it * step].coerceIn(-1f, 1f) }
}

// From an amplitude to 0..1 over the meter range of 60 dB.
private fun loudness(amplitude: Float): Float {
    if (amplitude <= 0f) return 0f
    val db = DB_PER_DECADE * log10(amplitude)
    return ((db - FLOOR_DB) / -FLOOR_DB).coerceIn(0f, 1f)
}

// The Hann window: 0.5 - 0.5 cos(2 pi n / (N - 1)). It keeps the edges of the window from smearing the bands.
private fun hann(index: Int, size: Int): Float = (HALF - HALF * cos(FULL_TURN * index / (size - 1))).toFloat()

private const val HALF = 0.5
private const val FULL_TURN = 2 * PI

/** An in place radix 2 fast Fourier transform. The sizes of both arrays are the same power of 2. */
internal fun fft(real: FloatArray, imaginary: FloatArray) {
    val size = real.size
    require(size > 0 && size and (size - 1) == 0) { "The size must be a power of 2" }
    var j = 0
    for (i in 1 until size) {
        var bit = size shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            real[i] = real[j].also { real[j] = real[i] }
            imaginary[i] = imaginary[j].also { imaginary[j] = imaginary[i] }
        }
    }
    var length = 2
    while (length <= size) {
        // The turn of one step of a wave of this length, clockwise.
        val angle = -FULL_TURN / length
        for (start in 0 until size step length) {
            for (k in 0 until length / 2) {
                val wr = cos(angle * k).toFloat()
                val wi = sin(angle * k).toFloat()
                val a = start + k
                val b = a + length / 2
                val tr = real[b] * wr - imaginary[b] * wi
                val ti = real[b] * wi + imaginary[b] * wr
                real[b] = real[a] - tr
                imaginary[b] = imaginary[a] - ti
                real[a] += tr
                imaginary[a] += ti
            }
        }
        length = length shl 1
    }
}

private const val LOWEST_HZ = 40f
private const val HIGHEST_HZ = 16_000f
private const val FLOOR_DB = -60f
private const val DB_PER_DECADE = 20f
private const val FULL_SCALE_DIVIDER = 4f

// The wave shows about 21 ms at 48 kHz: two or three periods of a low note.
private const val WAVE_SPAN = 1024
