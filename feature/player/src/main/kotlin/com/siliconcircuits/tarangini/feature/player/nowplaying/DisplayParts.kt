/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import com.siliconcircuits.tarangini.core.domain.model.VisualMode

/**
 * What the display shows besides the song: the visual and its mode, and whether volume levelling
 * is on for the LEVEL tag. [shown] is read only while the visual draws. [large] is true on a tall
 * screen such as a tablet held upright: the time and the visual are then bigger.
 */
internal data class DisplayParts(
    val mode: VisualMode = VisualMode.BARS,
    val levelling: Boolean = false,
    val shown: () -> ShownVisual = { ShownVisual.STILL },
    val onCycleVisual: () -> Unit = {},
    val large: Boolean = false,
)
