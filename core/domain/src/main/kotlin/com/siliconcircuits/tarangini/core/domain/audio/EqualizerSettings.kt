/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain.audio

import kotlin.math.log2

/** The largest boost or cut of one band, in dB. */
const val MAX_BAND_GAIN_DB = 12f

/** The largest bass boost, in dB. */
const val MAX_BASS_BOOST_DB = 12f

/** The centres of the five bands of the core build, in Hz, about two octaves apart. */
val FIVE_BANDS_HZ: List<Float> = listOf(60f, 230f, 910f, 3_600f, 14_000f)

// The curves of the presets, in dB at each band of FIVE_BANDS_HZ.
private val FLAT_CURVE = listOf(0f, 0f, 0f, 0f, 0f)
private val BASS_CURVE = listOf(6f, 4f, 0f, 0f, 0f)
private val VOCAL_CURVE = listOf(-2f, -1f, 3f, 4f, 1f)
private val CLASSICAL_CURVE = listOf(3f, 1f, -1f, 1f, 3f)

// The voice and the mridangam sit in the middle, the tambura and the violin above it.
private val CARNATIC_CURVE = listOf(1f, 2f, 3f, 2f, 1f)
private val BOLLYWOOD_CURVE = listOf(4f, 2f, 0f, 2f, 3f)

/** A gain the filters can use: inside the limits, and 0 for a value that is no number. */
private fun safeGain(gainDb: Float): Float =
    if (gainDb.isFinite()) gainDb.coerceIn(-MAX_BAND_GAIN_DB, MAX_BAND_GAIN_DB) else 0f

/**
 * A ready sound of the equalizer.
 *
 * A preset is a curve over the frequencies, not one value per band, so the same preset works for
 * five bands and for ten.
 *
 * @property curve the gain in dB at each frequency of [FIVE_BANDS_HZ]
 */
enum class EqualizerPreset(private val curve: List<Float>) {
    FLAT(FLAT_CURVE),
    BASS(BASS_CURVE),
    VOCAL(VOCAL_CURVE),
    CLASSICAL(CLASSICAL_CURVE),
    CARNATIC(CARNATIC_CURVE),
    BOLLYWOOD(BOLLYWOOD_CURVE),
    ;

    /**
     * The gain of this preset at [hz]. Between two points of the curve the gain changes in a
     * straight line over the octaves; below the first and above the last point it stays the same.
     */
    fun gainAt(hz: Float): Float {
        val points = FIVE_BANDS_HZ
        return when {
            hz <= points.first() -> curve.first()

            hz >= points.last() -> curve.last()

            else -> {
                val upper = points.indexOfFirst { it >= hz }
                val share = log2(hz / points[upper - 1]) / log2(points[upper] / points[upper - 1])
                curve[upper - 1] + share * (curve[upper] - curve[upper - 1])
            }
        }
    }
}

/**
 * The equalizer as the person set it.
 *
 * @property isOn false plays the sound as the file has it
 * @property preset the preset that was picked last, or null after a band was moved by hand
 * @property ownGains the gain of each band in dB, used when [preset] is null
 * @property bassBoost the bass boost in percent, from 0 to 100
 * @property genresOn true when a song of a genre in [genrePresets] plays with the preset of its genre
 * @property genrePresets the preset for a genre, by [com.siliconcircuits.tarangini.core.domain.model.Genre.key]
 * @property levelling how songs with ReplayGain tags are brought to a similar loudness; this works
 *   also while the equalizer is off
 */
data class EqualizerSettings(
    val isOn: Boolean = false,
    val preset: EqualizerPreset? = EqualizerPreset.FLAT,
    val ownGains: List<Float> = emptyList(),
    val bassBoost: Int = 0,
    val genresOn: Boolean = false,
    val genrePresets: Map<String, EqualizerPreset> = emptyMap(),
    val levelling: VolumeLevelling = VolumeLevelling.OFF,
) {
    /**
     * The gain in dB of each band at [bandsHz], each within [MAX_BAND_GAIN_DB]. Own gains that were
     * set for another number of bands give a flat sound.
     */
    fun gainsFor(bandsHz: List<Float>): List<Float> {
        val gains = when {
            preset != null -> bandsHz.map(preset::gainAt)
            ownGains.size == bandsHz.size -> ownGains
            else -> bandsHz.map { 0f }
        }
        return gains.map(::safeGain)
    }

    /**
     * The settings a song of [genreKeys] plays with: the preset of its first genre that has one, or
     * these settings. The bass boost is the same for every song.
     */
    fun forSong(genreKeys: List<String>): EqualizerSettings {
        val genrePreset = genreKeys.firstNotNullOfOrNull(genrePresets::get)
        return if (genresOn && genrePreset != null) copy(preset = genrePreset) else this
    }

    /** The same settings with [preset] for the genre [key]; null makes the genre follow the main setting. */
    fun withGenrePreset(key: String, preset: EqualizerPreset?): EqualizerSettings =
        copy(genrePresets = if (preset == null) genrePresets - key else genrePresets + (key to preset))

    /** The bass boost in dB. */
    fun bassBoostDb(): Float = bassBoost.coerceIn(0, MAX_BASS_PERCENT) * MAX_BASS_BOOST_DB / MAX_BASS_PERCENT

    /** The same settings with band [index] at [gainDb]. The preset is then gone: the bands are the person's own. */
    fun withBand(bandsHz: List<Float>, index: Int, gainDb: Float): EqualizerSettings {
        val gains = gainsFor(bandsHz).toMutableList()
        if (index in gains.indices) gains[index] = safeGain(gainDb)
        return copy(preset = null, ownGains = gains)
    }

    companion object {
        /** The bass boost at full, in percent. */
        const val MAX_BASS_PERCENT = 100
    }
}
