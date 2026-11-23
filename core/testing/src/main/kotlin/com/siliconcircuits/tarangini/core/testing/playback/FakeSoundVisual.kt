/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.testing.playback

import com.siliconcircuits.tarangini.core.domain.audio.SoundVisual
import com.siliconcircuits.tarangini.core.domain.audio.VisualFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A visual for tests. A test sets [frame]; it starts silent. */
class FakeSoundVisual : SoundVisual {
    /** The picture the visual shows now. */
    val frame = MutableStateFlow(VisualFrame.SILENT)

    override fun frames(): Flow<VisualFrame> = frame
}
