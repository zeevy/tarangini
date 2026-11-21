/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * How many lines a text of a list row may take. It is one line, so every row has the same height.
 * With a large font of the phone only a few letters fit on a line, so a text may then take two.
 */
@Composable
fun rowTextLines(): Int = if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) 2 else 1

private const val LARGE_FONT_SCALE = 1.5f
