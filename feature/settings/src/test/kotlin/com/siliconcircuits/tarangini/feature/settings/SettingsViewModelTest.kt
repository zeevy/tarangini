/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.settings

import com.siliconcircuits.tarangini.core.domain.library.ScanState
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.testing.MainDispatcherRule
import com.siliconcircuits.tarangini.core.testing.library.FakeLibraryScanner
import com.siliconcircuits.tarangini.core.testing.library.FakePlayHistory
import com.siliconcircuits.tarangini.core.testing.settings.FakeSettingsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val settings = FakeSettingsRepository().apply { folders = listOf("external_primary/Music") }
    private val scanner = FakeLibraryScanner()
    private val history = FakePlayHistory()

    private fun TestScope.watched(): SettingsViewModel =
        SettingsViewModel(settings, scanner, history).also { viewModel ->
            backgroundScope.launch { viewModel.state.collect() }
            runCurrent()
        }

    @Test
    fun `the screen shows what is left out and the folders with audio`() = runTest(mainDispatcher.dispatcher) {
        settings.scan.value = ScanSettings(30, setOf("external_primary/Calls"))

        val state = watched().state.value

        assertEquals(SettingsUiState(ScanSettings(30, setOf("external_primary/Calls")), settings.folders), state)
    }

    @Test
    fun `a change of what is left out is saved and starts a scan`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()

        viewModel.setMinTrackSeconds(60)
        viewModel.setFolderHidden("external_primary/Music", true)
        runCurrent()

        assertEquals(ScanSettings(60, setOf("external_primary/Music")), viewModel.state.value.scan)
        assertEquals(2, scanner.scanCount)
        assertTrue(viewModel.state.value.scanState is ScanState.Finished)
    }

    @Test
    fun `rescan scans, and the folders are read again after it`() = runTest(mainDispatcher.dispatcher) {
        val viewModel = watched()
        settings.folders = listOf("external_primary/Music", "external_primary/New")

        viewModel.rescan()
        runCurrent()

        assertEquals(1, scanner.scanCount)
        assertEquals(settings.folders, viewModel.state.value.folders)
    }

    @Test
    fun `clear history forgets every play`() = runTest(mainDispatcher.dispatcher) {
        history.songPlayed(1)
        val viewModel = watched()

        viewModel.clearHistory()
        runCurrent()

        assertEquals(emptyList<Long>(), history.played.value)
    }
}
