/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.playback.audio

import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.premium.EqualizerBands
import com.siliconcircuits.tarangini.core.domain.repository.EqualizerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Hands each change of the saved equalizer to [processor], from Settings, a restore or a new
 * start. The car hears the same, because it plays through the same player.
 *
 * The equalizer bands are out of the app for now (D91): only volume levelling is applied, and the
 * saved bands and presets are kept for when the equalizer comes back.
 */
internal fun followEqualizer(
    saved: EqualizerRepository,
    bands: EqualizerBands,
    processor: EqualizerProcessor,
    scope: CoroutineScope,
): Job = scope.launch {
    saved.settings().collect { settings -> processor.setTargets(appliedTargets(settings, bands.centresHz())) }
}

/** What is applied of [settings] while the equalizer is out of the app: volume levelling alone. */
internal fun appliedTargets(settings: EqualizerSettings, bandsHz: List<Float>): EqualizerTargets =
    targetsOf(settings.copy(isOn = false), bandsHz)

/** The main target and the target of each genre that has its own preset. */
internal fun targetsOf(settings: EqualizerSettings, bandsHz: List<Float>): EqualizerTargets {
    val byGenre = if (settings.isOn && settings.genresOn) {
        settings.genrePresets.mapValues { (key, _) -> targetOf(settings.forSong(listOf(key)), bandsHz) }
    } else {
        emptyMap()
    }
    return EqualizerTargets(targetOf(settings, bandsHz), byGenre, settings.levelling)
}

/** What the processor does for [settings]: nothing while the equalizer is off. */
internal fun targetOf(settings: EqualizerSettings, bandsHz: List<Float>): EqualizerTarget = if (settings.isOn) {
    // A band the filters cannot use (no number, or not above 0 Hz) is left out with its gain.
    val bands = bandsHz.filter { it.isFinite() && it > 0f }
    EqualizerTarget(bands, settings.gainsFor(bands), settings.bassBoostDb())
} else {
    EqualizerTarget.FLAT
}
