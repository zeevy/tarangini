/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.playback

import com.siliconcircuits.tarangini.core.domain.playback.ResumePlace
import com.siliconcircuits.tarangini.core.domain.playback.ResumeStore
import com.siliconcircuits.tarangini.core.domain.playback.SavedPlayback

/**
 * A [ResumeStore] for tests. It keeps what was saved in [saved] and counts
 * the saves of a whole queue in [queueSaves].
 */
class FakeResumeStore(var saved: SavedPlayback? = null) : ResumeStore {
    var queueSaves = 0
        private set

    override suspend fun saveQueue(playback: SavedPlayback) {
        queueSaves++
        saved = playback
    }

    override suspend fun savePlace(place: ResumePlace) {
        saved = saved?.copy(place = place)
    }

    override suspend fun load(): SavedPlayback? = saved
}
