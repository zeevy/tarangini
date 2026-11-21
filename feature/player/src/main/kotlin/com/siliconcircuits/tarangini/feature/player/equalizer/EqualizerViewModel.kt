/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.model.Genre
import com.siliconcircuits.tarangini.core.domain.model.Page
import com.siliconcircuits.tarangini.core.domain.premium.EqualizerBands
import com.siliconcircuits.tarangini.core.domain.repository.EqualizerRepository
import com.siliconcircuits.tarangini.core.domain.repository.GenreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What the equalizer screen shows.
 *
 * @property settings the equalizer as it is set
 * @property bandsHz the centre of each band in Hz
 * @property genres the named genres of the library, each of which can have its own preset
 */
internal data class EqualizerUiState(
    val settings: EqualizerSettings,
    val bandsHz: List<Float>,
    val genres: List<Genre> = emptyList(),
) {
    /** The gain of each band in dB, in the order of [bandsHz]. */
    val gains: List<Float> = settings.gainsFor(bandsHz)
}

/**
 * The equalizer screen. Each change is saved at once, and the playback service follows the saved
 * value, so the sound changes while the slider moves.
 */
@HiltViewModel
internal class EqualizerViewModel @Inject constructor(
    private val saved: EqualizerRepository,
    bands: EqualizerBands,
    genres: GenreRepository,
) : ViewModel() {
    private val bandsHz = bands.centresHz()

    // The last change made here. A slider sends many changes a second; the screen shows each one
    // at once and does not wait for the saved value to come back.
    private val changed = MutableStateFlow<EqualizerSettings?>(null)

    /** The settings, or null until they are read. */
    val state: StateFlow<EqualizerUiState?> = combine(saved.settings(), changed, namedGenres(genres)) {
            stored,
            own,
            named,
        ->
        EqualizerUiState(own ?: stored, bandsHz, named)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), null)

    /** Turns the equalizer on or off. */
    fun setOn(on: Boolean) = change { it.copy(isOn = on) }

    /** Picks [preset]; the bands follow it. */
    fun pickPreset(preset: EqualizerPreset) = change { it.copy(preset = preset) }

    /** Sets band [index] to [gainDb]. */
    fun setBand(index: Int, gainDb: Float) = change { it.withBand(bandsHz, index, gainDb) }

    /** Turns the presets per genre on or off. */
    fun setGenresOn(on: Boolean) = change { it.copy(genresOn = on) }

    /** Gives the genre [key] its own [preset]; null makes it follow the main setting. */
    fun setGenrePreset(key: String, preset: EqualizerPreset?) = change { it.withGenrePreset(key, preset) }

    /** Sets the bass boost to [percent]. */
    fun setBassBoost(percent: Int) =
        change { it.copy(bassBoost = percent.coerceIn(0, EqualizerSettings.MAX_BASS_PERCENT)) }

    private fun change(edit: (EqualizerSettings) -> EqualizerSettings) {
        val now = state.value?.settings ?: return
        val next = edit(now)
        if (next == now) return
        changed.value = next
        // The store writes one value after the other, so the newest is saved last. The write also
        // ends when the screen closes right after a move.
        viewModelScope.launch { withContext(NonCancellable) { saved.save(next) } }
    }

    /** The genres with a name, read once. A failed read shows no genres; the equalizer still works. */
    private fun namedGenres(genres: GenreRepository): Flow<List<Genre>> = flow {
        emit(emptyList())
        val read = runCatching { genres.genres(Page(limit = Page.MAX_LIMIT, offset = 0)) }
            .getOrElse { failure -> if (failure is CancellationException) throw failure else emptyList() }
        emit(read.filter { it.name.isNotEmpty() })
    }

    private companion object {
        const val STOP_AFTER_MS = 5_000L
    }
}
