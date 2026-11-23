/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.audio.SoundVisual
import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import com.siliconcircuits.tarangini.core.domain.audio.VolumeLevelling
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import com.siliconcircuits.tarangini.core.domain.repository.EqualizerRepository
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The bars or the wave of the player as it draws them: the [bands] and the [wave] of the sound,
 * and a [peaks] cap over each bar that falls slower than the bar.
 */
internal data class ShownVisual(val bands: List<Float>, val peaks: List<Float>, val wave: List<Float>) {
    companion object {
        val STILL = ShownVisual(VisualFrame.SILENT.bands, VisualFrame.SILENT.bands, VisualFrame.SILENT.wave)
    }
}

/**
 * The visual of the player and the LEVEL tag. The sound is read only while the visual is shown and
 * something plays: the screen says when through [setMoving].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class VisualViewModel @Inject constructor(
    visual: SoundVisual,
    private val settings: SettingsRepository,
    equalizer: EqualizerRepository,
) : ViewModel() {
    private val moving = MutableStateFlow(false)

    /** Bars, wave or off, as the person picked it last. */
    val mode: StateFlow<VisualMode> = settings.visualMode()
        .catch { emit(VisualMode.BARS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), VisualMode.BARS)

    /** True while volume levelling is on, for the LEVEL tag of the display. */
    val levelling: StateFlow<Boolean> = equalizer.settings()
        .map { it.levelling != VolumeLevelling.OFF }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), false)

    /** The picture to draw. It stands still while nothing plays or the visual is off. */
    val shown: StateFlow<ShownVisual> = combine(mode, moving) { mode, moving -> mode != VisualMode.OFF && moving }
        .flatMapLatest { reads ->
            if (reads) visual.frames().scan(ShownVisual.STILL, ::withPeaks) else flowOf(ShownVisual.STILL)
        }
        .catch { emit(ShownVisual.STILL) }
        // The sound is read only while the screen draws it, so a closed screen costs nothing.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ShownVisual.STILL)

    /** Tells whether something plays now, so the sound is read only then. */
    fun setMoving(on: Boolean) {
        moving.value = on
    }

    /** Moves to the next mode: bars, wave, off and bars again. The pick is kept. */
    fun cycle() {
        val next = VisualMode.entries[(mode.value.ordinal + 1) % VisualMode.entries.size]
        viewModelScope.launch { runCatching { settings.setVisualMode(next) } }
    }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}

/** The next picture: the bars of [frame], and each cap at the bar or a little below the cap before. */
internal fun withPeaks(before: ShownVisual, frame: VisualFrame): ShownVisual = ShownVisual(
    bands = frame.bands,
    peaks = List(frame.bands.size) { maxOf(frame.bands[it], before.peaks.getOrElse(it) { 0f } - PEAK_FALL) },
    wave = frame.wave,
)

// A cap falls the full height in about 2.5 s at 25 frames a second, slower than its bar.
private const val PEAK_FALL = 0.016f
