/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.settings

import com.siliconcircuits.tarangini.core.domain.model.SongSort
import com.siliconcircuits.tarangini.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Settings for tests, kept in memory. */
class FakeSettingsRepository : SettingsRepository {
    private val sort = MutableStateFlow(SongSort.TITLE)

    override fun songSort(): Flow<SongSort> = sort

    override suspend fun setSongSort(sort: SongSort) {
        this.sort.value = sort
    }
}
