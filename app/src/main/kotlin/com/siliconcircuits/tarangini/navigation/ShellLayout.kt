/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.navigation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How the shell lays out the tabs, from the width of the window. */
internal enum class ShellLayout {
    /** A phone held upright: the bottom bar. */
    COMPACT,

    /** A small tablet, an open foldable or a phone on its side: the rail. */
    MEDIUM,

    /** A tablet: the rail, and the now playing pane beside the tab screens. */
    EXPANDED,
    ;

    companion object {
        /**
         * The layout for a window [width] wide and [height] high, at the window size class limits of
         * Material. A large phone on its side is wide enough for the pane but too low for it: the cover
         * and the buttons would not fit, so it gets the rail only.
         */
        fun of(width: Dp, height: Dp): ShellLayout = when {
            width >= ExpandedWidth && height >= PaneHeight -> EXPANDED
            width >= MediumWidth -> MEDIUM
            else -> COMPACT
        }
    }
}

// The window size class limits of Material 3.
private val MediumWidth = 600.dp
private val ExpandedWidth = 840.dp
private val PaneHeight = 480.dp
