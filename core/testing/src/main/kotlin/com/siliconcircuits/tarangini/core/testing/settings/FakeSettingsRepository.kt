/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.settings

import com.siliconcircuits.tarangini.core.domain.model.Look
import com.siliconcircuits.tarangini.core.domain.model.ScanSettings
import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Settings for tests, kept in memory. */
class FakeSettingsRepository : SettingsRepository {
    /** The picked look. A test can read and set it. */
    val look = MutableStateFlow(Look.FOLLOW_PHONE)

    override fun look(): Flow<Look> = look

    override suspend fun setLook(look: Look) {
        this.look.value = look
    }

    /** What the player display shows. A test can read and set it. */
    val visual = MutableStateFlow(VisualMode.BARS)

    override fun visualMode(): Flow<VisualMode> = visual

    override suspend fun setVisualMode(mode: VisualMode) {
        visual.value = mode
    }

    private val sort = MutableStateFlow(SongSort.TITLE)

    override fun songSort(): Flow<SongSort> = sort

    override suspend fun setSongSort(sort: SongSort) {
        this.sort.value = sort
    }

    /** What is left out now. A test can read and set it. */
    val scan = MutableStateFlow(ScanSettings())

    /** The folders [audioFolders] gives. */
    var folders: List<String> = emptyList()

    override fun scanSettings(): Flow<ScanSettings> = scan

    override suspend fun setMinTrackSeconds(seconds: Int) {
        scan.value = scan.value.copy(minTrackSeconds = seconds)
    }

    override suspend fun setFolderHidden(path: String, hidden: Boolean) {
        val before = scan.value.hiddenFolders
        scan.value = scan.value.copy(hiddenFolders = if (hidden) before + path else before - path)
    }

    override suspend fun audioFolders(): List<String> = folders
}
