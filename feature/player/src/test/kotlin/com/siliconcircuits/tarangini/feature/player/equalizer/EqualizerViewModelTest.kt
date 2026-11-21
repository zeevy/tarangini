/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.EqualizerSettings
import com.siliconcircuits.tarangini.core.domain.audio.FIVE_BANDS_HZ
import com.siliconcircuits.tarangini.core.domain.premium.EqualizerBands
import com.siliconcircuits.tarangini.core.testing.library.FakeGenreRepository
import com.siliconcircuits.tarangini.core.testing.library.testSong
import com.siliconcircuits.tarangini.core.testing.settings.FakeEqualizerRepository
import com.siliconcircuits.tarangini.feature.player.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class EqualizerViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val saved = FakeEqualizerRepository()
    private val genres = FakeGenreRepository().apply {
        songsByGenre =
            mapOf("Carnatic" to listOf(testSong(1)), "Rock" to listOf(testSong(2)), "" to listOf(testSong(3)))
    }

    // Made at first use, after the rule has set the main dispatcher.
    private val viewModel by lazy { EqualizerViewModel(saved, EqualizerBands { FIVE_BANDS_HZ }, genres) }

    @Test
    fun `the screen shows the saved settings with the bands of the build`() = runTest(mainDispatcher.dispatcher) {
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()

        val state = viewModel.state.value!!
        assertEquals(EqualizerSettings(), state.settings)
        assertEquals(FIVE_BANDS_HZ, state.bandsHz)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), state.gains)
    }

    @Test
    fun `each change is saved at once`() = runTest(mainDispatcher.dispatcher) {
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()

        viewModel.setOn(true)
        viewModel.pickPreset(EqualizerPreset.CARNATIC)
        viewModel.setBassBoost(30)

        assertEquals(
            EqualizerSettings(isOn = true, preset = EqualizerPreset.CARNATIC, bassBoost = 30),
            saved.saved.value,
        )
        assertEquals(3, saved.saves)
    }

    @Test
    fun `moving a band leaves the preset and keeps the other bands`() = runTest(mainDispatcher.dispatcher) {
        saved.saved.value = EqualizerSettings(isOn = true, preset = EqualizerPreset.BASS)
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()

        viewModel.setBand(4, 3f)

        val state = viewModel.state.value!!
        assertNull(state.settings.preset)
        assertEquals(listOf(6f, 4f, 0f, 0f, 3f), state.gains)
        assertEquals(state.settings, saved.saved.value)
    }

    @Test
    fun `the screen lists the named genres, and a genre gets its own preset`() = runTest(mainDispatcher.dispatcher) {
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()

        assertEquals(listOf("Carnatic", "Rock"), viewModel.state.value!!.genres.map { it.name })

        viewModel.setGenresOn(true)
        viewModel.setGenrePreset("Rock", EqualizerPreset.BASS)
        viewModel.setGenrePreset("Carnatic", EqualizerPreset.CARNATIC)
        viewModel.setGenrePreset("Carnatic", null)

        assertEquals(true, saved.saved.value.genresOn)
        assertEquals(mapOf("Rock" to EqualizerPreset.BASS), saved.saved.value.genrePresets)
    }

    @Test
    fun `the same value again is not saved again`() = runTest(mainDispatcher.dispatcher) {
        backgroundScope.launch { viewModel.state.collect() }
        runCurrent()

        viewModel.setOn(false)
        viewModel.setBassBoost(0)

        assertEquals(0, saved.saves)
    }
}
