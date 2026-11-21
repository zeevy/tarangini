/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.domain

import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.audio.FIVE_BANDS_HZ
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EqualizerSettingsTest {
    @Test
    fun `a preset gives its own values at the five bands`() {
        assertEquals(
            listOf(6f, 4f, 0f, 0f, 0f),
            EqualizerSettings(preset = EqualizerPreset.BASS).gainsFor(FIVE_BANDS_HZ),
        )
    }

    @Test
    fun `between two points the gain changes over the octaves, and stays flat outside them`() {
        // 120 Hz is one octave above 60 Hz, a little more than half of the 1.94 octaves to 230 Hz.
        assertEquals(4.97f, EqualizerPreset.BASS.gainAt(120f), 0.01f)
        assertEquals(6f, EqualizerPreset.BASS.gainAt(20f), 0f)
        assertEquals(3f, EqualizerPreset.CLASSICAL.gainAt(20_000f), 0f)
    }

    @Test
    fun `ten bands follow the same curve`() {
        val tenBands = listOf(31f, 62f, 125f, 250f, 500f, 1_000f, 2_000f, 4_000f, 8_000f, 16_000f)

        val gains = EqualizerSettings(preset = EqualizerPreset.VOCAL).gainsFor(tenBands)

        assertEquals(10, gains.size)
        assertEquals(-2f, gains.first(), 0f)
        assertEquals(1f, gains.last(), 0f)
    }

    @Test
    fun `moving a band makes the bands the person's own`() {
        val moved = EqualizerSettings(preset = EqualizerPreset.BASS).withBand(FIVE_BANDS_HZ, index = 2, gainDb = 30f)

        assertNull(moved.preset)
        assertEquals(listOf(6f, 4f, 12f, 0f, 0f), moved.gainsFor(FIVE_BANDS_HZ))
    }

    @Test
    fun `a song plays with the preset of its first genre that has one`() {
        val settings = EqualizerSettings(
            isOn = true,
            preset = EqualizerPreset.VOCAL,
            bassBoost = 30,
            genresOn = true,
            genrePresets = mapOf("carnatic" to EqualizerPreset.CARNATIC, "rock" to EqualizerPreset.BASS),
        )

        assertEquals(EqualizerPreset.CARNATIC, settings.forSong(listOf("folk", "carnatic", "rock")).preset)
        assertEquals(30, settings.forSong(listOf("carnatic")).bassBoost)
        assertEquals(settings, settings.forSong(listOf("folk")))
        assertEquals(settings, settings.forSong(emptyList()))
    }

    @Test
    fun `with the genre switch off every song plays with the main setting`() {
        val settings = EqualizerSettings(isOn = true, genrePresets = mapOf("rock" to EqualizerPreset.BASS))

        assertEquals(settings, settings.forSong(listOf("rock")))
    }

    @Test
    fun `a genre gets a preset, and back to the main setting with none`() {
        val rock = EqualizerSettings().withGenrePreset("rock", EqualizerPreset.BASS)

        assertEquals(mapOf("rock" to EqualizerPreset.BASS), rock.genrePresets)
        assertEquals(emptyMap<String, EqualizerPreset>(), rock.withGenrePreset("rock", null).genrePresets)
    }

    @Test
    fun `a gain that is no number is flat`() {
        val own = EqualizerSettings(preset = null, ownGains = listOf(Float.NaN, Float.POSITIVE_INFINITY, 2f, 0f, 0f))

        assertEquals(listOf(0f, 0f, 2f, 0f, 0f), own.gainsFor(FIVE_BANDS_HZ))
    }

    @Test
    fun `own gains for another number of bands give a flat sound`() {
        val own = EqualizerSettings(preset = null, ownGains = listOf(3f, 3f))

        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), own.gainsFor(FIVE_BANDS_HZ))
    }

    @Test
    fun `the bass boost goes from 0 to 12 dB`() {
        assertEquals(0f, EqualizerSettings(bassBoost = 0).bassBoostDb(), 0f)
        assertEquals(6f, EqualizerSettings(bassBoost = 50).bassBoostDb(), 0.001f)
        assertEquals(12f, EqualizerSettings(bassBoost = 140).bassBoostDb(), 0f)
    }
}
