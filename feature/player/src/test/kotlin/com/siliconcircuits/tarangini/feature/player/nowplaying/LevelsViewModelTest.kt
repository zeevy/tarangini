/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import com.siliconcircuits.tarangini.core.domain.audio.LevelReading
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.playback.FakeAudioLevels
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LevelsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun `the meter shows silence first and then each new level`() = runTest(mainDispatcher.dispatcher) {
        val levels = FakeAudioLevels()
        val viewModel = LevelsViewModel(levels)
        backgroundScope.launch { viewModel.levels.collect() }
        runCurrent()
        assertEquals(LevelReading.SILENCE, viewModel.levels.value)

        levels.reading.value = LevelReading(-6f, -12f)
        runCurrent()

        assertEquals(LevelReading(-6f, -12f), viewModel.levels.value)
    }
}
