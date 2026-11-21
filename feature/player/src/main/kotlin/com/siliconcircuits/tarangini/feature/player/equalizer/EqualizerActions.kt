/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.equalizer

import com.siliconcircuits.tarangini.core.domain.audio.EqualizerPreset
import com.siliconcircuits.tarangini.core.domain.audio.VolumeLevelling

/** What the parts of the equalizer screen do. */
internal data class EqualizerActions(
    val onSetOn: (Boolean) -> Unit = {},
    val onPickPreset: (EqualizerPreset) -> Unit = {},
    val onSetBand: (Int, Float) -> Unit = { _, _ -> },
    val onSetBassBoost: (Int) -> Unit = {},
    val onSetGenresOn: (Boolean) -> Unit = {},
    val onSetGenrePreset: (String, EqualizerPreset?) -> Unit = { _, _ -> },
    val onSetLevelling: (VolumeLevelling) -> Unit = {},
)
